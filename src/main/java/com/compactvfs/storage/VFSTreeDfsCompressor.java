package com.compactvfs.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import org.apache.commons.lang3.SerializationUtils;

public class VFSTreeDfsCompressor {

    public static int compress(RandomAccessFile out, VFSDirectory rootDirectory) throws IOException {
        int bytesCnt = 0;
        bytesCnt += writeObject(out, rootDirectory.getPath());
        out.writeInt(rootDirectory.getSubFiles().size());
        bytesCnt += Integer.BYTES;
        for (VFSFile file : rootDirectory.getSubFiles()) {
            bytesCnt += writeObject(out, file.getPath());
        }
        out.writeInt(rootDirectory.getSubDirectories().size());
        bytesCnt += Integer.BYTES;
        for (VFSDirectory directory : rootDirectory.getSubDirectories()) {
            bytesCnt += compress(out, directory);
        }
        return bytesCnt;
    }

    public static int countTreeBytesCount(VFSDirectory rootDirectory) {
        int bytesCnt = 0;
        bytesCnt += bytesCount(rootDirectory.getPath());
        bytesCnt += Integer.BYTES;
        for (VFSFile file : rootDirectory.getSubFiles()) {
            bytesCnt += bytesCount(file.getPath());
        }
        bytesCnt += Integer.BYTES;
        for (VFSDirectory directory : rootDirectory.getSubDirectories()) {
            bytesCnt += countTreeBytesCount(directory);
        }
        return bytesCnt;
    }

    public static VFSDirectory decompress(RandomAccessFile in) throws IOException, ClassNotFoundException {
        String rootDirectoryPath = (String) readObject(in);
        int filesCount = in.readInt();
        List<VFSFile> files = new ArrayList<>();
        for (int i = 0; i < filesCount; i++) {
            String filePath = (String) readObject(in);
            files.add(new VFSFile(filePath));
        }
        int dirsCount = in.readInt();
        List<VFSDirectory> directories = new ArrayList<>();
        for (int i = 0; i < dirsCount; i++) {
            directories.add(decompress(in));
        }
        return new VFSDirectory(rootDirectoryPath, new TreeSet<>(directories), new TreeSet<>(files));
    }

    /**
     * @return written bytes count
     */
    public static <T extends Serializable> int writeObject(RandomAccessFile out, T object) throws IOException {
        byte[] data = SerializationUtils.serialize(object);
        out.writeInt(data.length);
        out.write(data, 0, data.length);
        return Integer.BYTES + data.length;
    }

    public static <T extends Serializable> int bytesCount(T object) {
        byte[] data = SerializationUtils.serialize(object);
        return Integer.BYTES + data.length;
    }

    public static Object readObject(RandomAccessFile in) throws IOException {
        int objectBytesCount = in.readInt();
        byte[] data = new byte[objectBytesCount];
        in.read(data, 0, objectBytesCount);
        return SerializationUtils.deserialize(data);
    }
}
