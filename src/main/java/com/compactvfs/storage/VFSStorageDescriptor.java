package com.compactvfs.storage;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.compactvfs.model.VFS;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;

public class VFSStorageDescriptor {
    private final RandomAccessFile storage;
    private final Map<String, Long> fileContentPosition;

    private VFSStorageDescriptor(
            Path dirPathToStore,
            String fileName
    ) throws IOException {
        storage = new RandomAccessFile(dirPathToStore.toString() + "/" + fileName, "rw");
        storage.setLength(0);
        fileContentPosition = new HashMap<>();
    }

    private VFSStorageDescriptor(String filePath) throws FileNotFoundException {
        storage = new RandomAccessFile(filePath, "rw");
        fileContentPosition = new HashMap<>();
    }

    public static VFSStorageDescriptor save(VFSDirectory vfsDirectory, Path dirPathToStore) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(
                dirPathToStore,
                "descriptor_" + vfsDirectory.getName() + ".ser"
        );

        Stack<VFSDirectory> dfsStack = new Stack<>();
        dfsStack.push(vfsDirectory);

        FileDescriptor descriptor = vfsStorageDescriptor.getStorage().getFD();
        try (FileOutputStream fileOutputStream = new FileOutputStream(descriptor)) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                VFSTreeDfsCompressor.compress(objectOutputStream, vfsDirectory);

                while (!dfsStack.isEmpty()) {
                    VFSDirectory currentDir = dfsStack.pop();

                    for (VFSFile subFile : currentDir.getSubFiles()) {
                        try {
                            vfsStorageDescriptor.writeFileContent(
                                    subFile,
                                    fileOutputStream,
                                    objectOutputStream,
                                    vfsStorageDescriptor.getFileContentPosition()
                            );
                        } catch (IOException ex) {
                            System.out.println("File skipped. Can't store file with path: " + subFile.getPath());
                        }
                    }

                    for (VFSDirectory subDir : currentDir.getSubDirectories()) {
                        dfsStack.push(subDir);
                    }
                }
            }
        }

        return vfsStorageDescriptor;
    }

    public static VFS load(Path descriptorPath) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(descriptorPath.toString());

        FileDescriptor descriptor = vfsStorageDescriptor.getStorage().getFD();
        VFSDirectory vfsDirectory;
        try (FileInputStream fileInputStream = new FileInputStream(descriptor)) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                try {
                    vfsDirectory = VFSTreeDfsCompressor.decompress(objectInputStream);
                    readFileContentPositionMap(
                            fileInputStream,
                            objectInputStream,
                            vfsStorageDescriptor.getFileContentPosition()
                    );
                } catch (ClassNotFoundException ex) {
                    throw new IOException("Can't decompress VFS tree, ex: " + ex.getMessage());
                }
            }
        }
        return new VFS(
                vfsDirectory,
                vfsStorageDescriptor
        );
    }

    public RandomAccessFile getStorage() {
        return storage;
    }

    public Map<String, Long> getFileContentPosition() {
        return fileContentPosition;
    }

    public byte[] readFileContent(VFSFile vfsFile) throws IOException {
        Long fileContentPos = fileContentPosition.get(vfsFile.getPath());
        if (fileContentPos == null) {
            throw new IOException("No vfsFile content with path: " + vfsFile.getPath());
        }
        storage.seek(fileContentPos);
        int contentBytesCount = storage.readInt();
        byte[] fileContent = new byte[contentBytesCount];
        storage.read(fileContent, 0, contentBytesCount);
        return fileContent;
    }

    private void writeFileContent(
            VFSFile vfsFile,
            FileOutputStream fileOutputStream,
            ObjectOutput objectOutput,
            Map<String, Long> fileContentPosition
    ) throws IOException {
        String vfsFilePath = vfsFile.getPath();

        objectOutput.writeObject(vfsFilePath);

        long currentPosition = fileOutputStream.getChannel().position();
        fileContentPosition.put(
                vfsFilePath,
                currentPosition
        );

        objectOutput.writeInt(vfsFile.getContent().length);
        objectOutput.write(vfsFile.getContent());
    }

    private static void readFileContentPositionMap(
            FileInputStream fileInputStream,
            ObjectInput objectInput,
            Map<String, Long> fileContentPosition
    ) throws IOException, ClassNotFoundException {
        while (fileInputStream.available() > 0) {
            String filePath = (String) objectInput.readObject();
            long currentPosition = fileInputStream.getChannel().position();
            fileContentPosition.put(filePath, currentPosition);
            int contentBytesCount = objectInput.readInt();
            byte[] fileContentIgnored = new byte[contentBytesCount];
            objectInput.read(fileContentIgnored, 0, contentBytesCount);
        }
    }

}
