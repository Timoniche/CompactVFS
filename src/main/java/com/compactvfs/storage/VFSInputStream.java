package com.compactvfs.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class VFSInputStream implements AutoCloseable {
    private final List<Long> contentChunkPositions;
    private final RandomAccessFile storage;

    private int posInChunkContent = 0;
    private int chunkIndex = 0;


    public VFSInputStream(List<Long> contentChunkPositions, String storagePath) throws IOException {
        this.contentChunkPositions = contentChunkPositions;
        if (contentChunkPositions == null || contentChunkPositions.isEmpty()) {
            throw new IOException("No vfsFile content, storage: " + storagePath);
        }
        storage = new RandomAccessFile(storagePath, "r");
    }

    /**
     * readNBytes stores last call position
     * moves position to zero if file is fully read
     *
     * @param b size >= n
     * @return count of read bytes
     */
    public int readNBytes(byte[] b, int n) throws IOException {
        int nChunks = contentChunkPositions.size();
        int bytesLeft = n;
        while (bytesLeft >= 0) {
            if (chunkIndex >= nChunks) {
                seekBegin();
                return n - bytesLeft;
            }
            long chunkInitPos = contentChunkPositions.get(chunkIndex);
            storage.seek(chunkInitPos);
            int chunkContentBytesCount = storage.readInt();
            int chunkContentBytesLeft = chunkContentBytesCount - posInChunkContent;

            storage.seek(chunkInitPos + Integer.BYTES + posInChunkContent);
            if (bytesLeft < chunkContentBytesLeft) {
                storage.read(b, n - bytesLeft, bytesLeft);
                posInChunkContent += bytesLeft;
                return n;
            } else {
                storage.read(b, n - bytesLeft, chunkContentBytesLeft);
                bytesLeft -= chunkContentBytesLeft;
                chunkIndex++;
                posInChunkContent = 0;
            }
        }
        throw new AssertionError("Read must be completed");
    }

    private void seekBegin() {
        chunkIndex = 0;
        posInChunkContent = 0;
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
    public void close() throws IOException {
        storage.close();
    }
}

