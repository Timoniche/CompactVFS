package com.compactvfs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;

import static com.compactvfs.model.VFSDirectory.ROOT_PREFIX_PATH;

public class FSAdapter {

    public static VFSDirectory fromFS(Path dirPath) {
        return fromFS(dirPath, ROOT_PREFIX_PATH + dirPath.getFileName());
    }

    private static VFSDirectory fromFS(Path dirPath, String vfsPath) {
        VFSDirectory rootVfsDirectory = new VFSDirectory(vfsPath, new TreeSet<>(), new TreeSet<>());
        try (Stream<Path> subPaths = Files.list(dirPath)) {
            subPaths.forEach(
                    curPath -> {
                        File curFile = curPath.toFile();
                        String curVfsPath = vfsPath + "/" + curFile.getName();
                        if (Files.isDirectory(curPath)) {
                            VFSDirectory childVfsDirectory = fromFS(curPath, curVfsPath);
                            rootVfsDirectory.getSubDirectories().add(childVfsDirectory);
                        } else {
                            try {
                                byte[] fileContent = Files.readAllBytes(curPath);
                                VFSFile vfsFile = new VFSFile(curVfsPath);
                                vfsFile.setContent(fileContent);
                                rootVfsDirectory.getSubFiles().add(vfsFile);
                            } catch (IOException ex) {
                                System.out.println("Can't load content from path: " +
                                        curPath + ", file will be missed in FS, ex: " + ex.getMessage()
                                );
                            }
                        }
                    }
            );
        } catch (IOException ex) {
            System.out.println("Can't load FS due to " + ex.getMessage());
            return null;
        }
        return rootVfsDirectory;
    }

    public static void toFS(VFSDirectory vfsDirectory, Path dirPath) {
        toFSHelper(vfsDirectory, Paths.get(dirPath.toString(), vfsDirectory.getName()));
    }

    private static void toFSHelper(VFSDirectory vfsDirectory, Path rootDirPath) {
        try {
            Files.createDirectories(rootDirPath);
            for (VFSFile vfsSubFile : vfsDirectory.getSubFiles()) {
                Path fsSubFilePath = Paths.get(rootDirPath + "/" + vfsSubFile.getName());
                try {
                    Files.write(fsSubFilePath, vfsSubFile.getContent());
                } catch (IOException ex) {
                    System.out.println("Can't write to/create file with path " + fsSubFilePath);
                }
            }
            for (VFSDirectory vfsSubDirectory : vfsDirectory.getSubDirectories()) {
                Path fsSubDirPath = Paths.get(rootDirPath + "/" + vfsSubDirectory.getName());
                toFSHelper(vfsSubDirectory, fsSubDirPath);
            }
        } catch (IOException ex) {
            System.out.println("Can't create directory with path " + rootDirPath);
        }
    }

}
