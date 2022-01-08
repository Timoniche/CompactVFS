package com.compactvfs.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.compactvfs.model.VFS;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;

import static com.compactvfs.storage.VFSTreeDfsCompressor.readObject;
import static com.compactvfs.storage.VFSTreeDfsCompressor.writeObject;

public class VFSStorageDescriptor {
    private final Map<String, Long> fileContentPosition;
    private final String storagePath;

    private VFSStorageDescriptor(
            Path dirPathToStore,
            String fileName
    ) {
        storagePath = dirPathToStore.toString() + "/" + fileName;
        fileContentPosition = new HashMap<>();
    }

    private VFSStorageDescriptor(String filePath) {
        fileContentPosition = new HashMap<>();
        storagePath = filePath;
    }

    public static VFSStorageDescriptor save(VFSDirectory vfsDirectory, Path dirPathToStore) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(
                dirPathToStore,
                "descriptor_" + vfsDirectory.getName() + ".ser"
        );

        Stack<VFSDirectory> dfsStack = new Stack<>();
        dfsStack.push(vfsDirectory);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(vfsStorageDescriptor.storagePath, "rw")) {
            randomAccessFile.setLength(0);

            VFSTreeDfsCompressor.compress(randomAccessFile, vfsDirectory);

            while (!dfsStack.isEmpty()) {
                VFSDirectory currentDir = dfsStack.pop();

                for (VFSFile subFile : currentDir.getSubFiles()) {
                    try {
                        vfsStorageDescriptor.writeFileContent(
                                subFile,
                                vfsStorageDescriptor.getFileContentPosition(),
                                randomAccessFile
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

        return vfsStorageDescriptor;
    }

    public static VFS load(Path descriptorPath) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(descriptorPath.toString());

        VFSDirectory vfsDirectory;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(vfsStorageDescriptor.storagePath, "rw")) {
            try {
                vfsDirectory = VFSTreeDfsCompressor.decompress(randomAccessFile);
                readFileContentPositionMap(
                        vfsStorageDescriptor.getFileContentPosition(),
                        randomAccessFile
                );
            } catch (ClassNotFoundException ex) {
                throw new IOException("Can't decompress VFS tree, ex: " + ex.getMessage());
            }

        }
        return new VFS(
                vfsDirectory,
                vfsStorageDescriptor
        );
    }

    public Map<String, Long> getFileContentPosition() {
        return fileContentPosition;
    }

    public byte[] readFileContent(VFSFile vfsFile) throws IOException {
        // fseek (from C) under the hood + sparse files are usually supported
        try (RandomAccessFile storage = new RandomAccessFile(storagePath, "rw")) {
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
    }

    private void writeFileContent(
            VFSFile vfsFile,
            Map<String, Long> fileContentPosition,
            RandomAccessFile storage
    ) throws IOException {
        String vfsFilePath = vfsFile.getPath();

        writeObject(storage, vfsFilePath);

        long currentPosition = storage.getChannel().position();
        fileContentPosition.put(
                vfsFilePath,
                currentPosition
        );

        storage.writeInt(vfsFile.getContent().length);
        storage.write(vfsFile.getContent(), 0, vfsFile.getContent().length);
    }

    private static void readFileContentPositionMap(
            Map<String, Long> fileContentPosition,
            RandomAccessFile storage
    ) throws IOException, ClassNotFoundException {
        long endPos = storage.length();
        while (storage.getChannel().position() < endPos) {
            String filePath = (String) readObject(storage);
            long currentPosition = storage.getChannel().position();
            fileContentPosition.put(filePath, currentPosition);
            int contentBytesCount = storage.readInt();
            byte[] fileContentIgnored = new byte[contentBytesCount];
            storage.read(fileContentIgnored, 0, contentBytesCount);
        }
    }

}
