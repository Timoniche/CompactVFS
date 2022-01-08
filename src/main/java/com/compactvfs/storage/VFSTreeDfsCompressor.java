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

    public static void compress(RandomAccessFile out, VFSDirectory rootDirectory) throws IOException {
        writeObject(out, rootDirectory.getPath());
        out.writeInt(rootDirectory.getSubFiles().size());
        for (VFSFile file : rootDirectory.getSubFiles()) {
            writeObject(out, file.getPath());
        }
        out.writeInt(rootDirectory.getSubDirectories().size());
        for (VFSDirectory directory : rootDirectory.getSubDirectories()) {
            compress(out, directory);
        }
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

    public static <T extends Serializable> void writeObject(RandomAccessFile out, T object) throws IOException {
        byte[] data = SerializationUtils.serialize(object);
        out.writeInt(data.length);
        out.write(data, 0, data.length);
    }

    public static Object readObject(RandomAccessFile in) throws IOException {
        int objectBytesCount = in.readInt();
        byte[] data = new byte[objectBytesCount];
        in.read(data, 0, objectBytesCount);
        return SerializationUtils.deserialize(data);
    }
}
