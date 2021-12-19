package com.compactvfs;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;

public class VFSTreeDfsCompressor {

    public static void compress(ObjectOutput out, VFSDirectory rootDirectory) throws IOException {
        out.writeObject(rootDirectory.getPath());
        out.writeInt(rootDirectory.getSubFiles().size());
        for (VFSFile file : rootDirectory.getSubFiles()) {
            out.writeObject(file.getPath());
        }
        out.writeInt(rootDirectory.getSubDirectories().size());
        for (VFSDirectory directory : rootDirectory.getSubDirectories()) {
            compress(out, directory);
        }
    }

    public static VFSDirectory decompress(ObjectInput in) throws IOException, ClassNotFoundException {
        String rootDirectoryPath = (String) in.readObject();
        int filesCount = in.readInt();
        List<VFSFile> files = new ArrayList<>();
        for (int i = 0; i < filesCount; i++) {
            String filePath = (String) in.readObject();
            files.add(new VFSFile(filePath));
        }
        int dirsCount = in.readInt();
        List<VFSDirectory> directories = new ArrayList<>();
        for (int i = 0; i < dirsCount; i++) {
            directories.add(decompress(in));
        }
        return new VFSDirectory(rootDirectoryPath, new TreeSet<>(directories), new TreeSet<>(files));
    }
}
