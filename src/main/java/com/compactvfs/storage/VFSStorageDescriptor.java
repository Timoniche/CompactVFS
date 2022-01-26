package com.compactvfs.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.compactvfs.model.VFS;
import com.compactvfs.model.VFSDirectory;

import static com.compactvfs.storage.VFSTreeDfsCompressor.readObject;
import static com.compactvfs.storage.VFSTreeDfsCompressor.writeObject;

public class VFSStorageDescriptor {
    private static final int BYTES_FOR_TREE = 100_000;

    private final Map<String, List<Long>> fileContentChunkPositions;
    private final String storagePath;

    private VFSStorageDescriptor(
            Path dirPathToStore,
            String fileName
    ) throws IOException {
        Files.createDirectories(dirPathToStore);
        storagePath = dirPathToStore + "/" + fileName;
        fileContentChunkPositions = new HashMap<>();
    }

    private VFSStorageDescriptor(String filePath) throws IOException {
        Files.createDirectories(Paths.get(filePath).getParent());
        fileContentChunkPositions = new HashMap<>();
        storagePath = filePath;
    }

    public static VFSStorageDescriptor initTreeFrom(VFSDirectory vfsDirectory, Path dirPathToStore) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(
                dirPathToStore,
                "descriptor_" + vfsDirectory.getName() + ".ser"
        );
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(vfsStorageDescriptor.storagePath, "rw")) {
            randomAccessFile.setLength(0);
            vfsStorageDescriptor.writeVfsTree(vfsDirectory, randomAccessFile);
            int curBytesForTree = (int) randomAccessFile.length();
            randomAccessFile.writeInt(curBytesForTree);
            byte[] zerosGap = new byte[BYTES_FOR_TREE - curBytesForTree - Integer.BYTES];
            randomAccessFile.write(zerosGap, 0, zerosGap.length);
        }
        return vfsStorageDescriptor;
    }

    public void clearStorage() throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(storagePath, "rw")) {
            randomAccessFile.setLength(0);
        }
    }

    private void writeVfsTree(
            VFSDirectory vfsDirectory,
            RandomAccessFile storage
    ) throws IOException {
        VFSTreeDfsCompressor.compress(storage, vfsDirectory);
    }

    public static VFS load(Path descriptorPath) throws IOException {
        VFSStorageDescriptor vfsStorageDescriptor = new VFSStorageDescriptor(descriptorPath.toString());

        VFSDirectory vfsDirectory;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(vfsStorageDescriptor.storagePath, "r")) {
            try {
                vfsDirectory = VFSTreeDfsCompressor.decompress(randomAccessFile);
                int treeBytesCnt = randomAccessFile.readInt();
                byte[] zerosGap = new byte[BYTES_FOR_TREE - treeBytesCnt - Integer.BYTES];
                randomAccessFile.read(zerosGap, 0, zerosGap.length);
                readFileContentPositionMap(
                        vfsStorageDescriptor.getFileContentChunkPositions(),
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

    public Map<String, List<Long>> getFileContentChunkPositions() {
        return fileContentChunkPositions;
    }

    public VFSInputStream readFileContent(String vfsFilePath) throws IOException {
        // fseek (from C) under the hood + sparse files are usually supported
        List<Long> fileContentChunkPoss = fileContentChunkPositions.get(vfsFilePath);
        return new VFSInputStream(fileContentChunkPoss, storagePath);
    }

    // os dependent FileChannel.write (Windows can't be parallelized?)
    public void writeNewFileContentInTheEnd(
            String vfsFilePath,
            byte[] newContent
    ) throws IOException {
        try (RandomAccessFile storage = new RandomAccessFile(storagePath, "rw")) {
            long endPos = storage.length();
            storage.seek(endPos);

            writeObject(storage, vfsFilePath);

            ArrayList<Long> contentPoss = new ArrayList<>();
            contentPoss.add(storage.getChannel().position());
            fileContentChunkPositions.put(
                    vfsFilePath,
                    contentPoss
            );

            storage.writeInt(newContent.length);
            storage.write(newContent, 0, newContent.length);
        }
    }

    public void writeBytesToTheEndOfFile(
            String vfsFilePath,
            byte[] additionalContent
    ) throws IOException {
        try (RandomAccessFile storage = new RandomAccessFile(storagePath, "rw")) {
            long endPos = storage.length();
            storage.seek(endPos);

            writeObject(storage, vfsFilePath);

            fileContentChunkPositions
                    .computeIfAbsent(vfsFilePath, k -> new ArrayList<>())
                    .add(storage.getChannel().position());

            storage.writeInt(additionalContent.length);
            storage.write(additionalContent, 0, additionalContent.length);
        }
    }

    private static void readFileContentPositionMap(
            Map<String, List<Long>> fileContentChunkPositions,
            RandomAccessFile storage
    ) throws IOException, ClassNotFoundException {
        long endPos = storage.length();
        while (storage.getChannel().position() < endPos) {
            String filePath = (String) readObject(storage);
            long currentPosition = storage.getChannel().position();
            fileContentChunkPositions
                    .computeIfAbsent(filePath, k -> new ArrayList<>())
                    .add(currentPosition);
            int contentBytesCount = storage.readInt();
            byte[] fileContentIgnored = new byte[contentBytesCount];
            storage.read(fileContentIgnored, 0, contentBytesCount);
        }
    }

}
