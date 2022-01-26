package com.compactvfs.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class VFSInputStream implements AutoCloseable {
    private final List<Long> contentChunkPositions;
    private final RandomAccessFile storage;

    public VFSInputStream(List<Long> contentChunkPositions, String storagePath) throws IOException {
        this.contentChunkPositions = contentChunkPositions;
        if (contentChunkPositions == null || contentChunkPositions.isEmpty()) {
            throw new IOException("No vfsFile content, storage: " + storagePath);
        }
        storage = new RandomAccessFile(storagePath, "r");
    }

    public byte[] readAllBytes() throws IOException {
        ByteArrayOutputStream contentStream = new ByteArrayOutputStream();

        for (Long chunkPos : contentChunkPositions) {
            storage.seek(chunkPos);
            int contentBytesCount = storage.readInt();
            byte[] chunkContent = new byte[contentBytesCount];
            storage.read(chunkContent, 0, contentBytesCount);
            contentStream.write(chunkContent);
        }
        return contentStream.toByteArray();
    }

    @Override
    public void close() throws Exception {
        storage.close();
    }
}

