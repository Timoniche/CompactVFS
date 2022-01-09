package com.compactvfs.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.compactvfs.model.VFS;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;

import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.model.VFSDirectory.emptyWithPath;
import static com.compactvfs.storage.VFSStorageDescriptor.initTreeFrom;
import static java.util.stream.Collectors.toList;

public class FSAdapter {

    public static VFS fromFS(Path dirPath, Path descriptorDirPath) throws IOException {
        String vfsPath = VFS_PREFIX_PATH + dirPath.getFileName();
        VFSDirectory ignoredEmptyTree = emptyWithPath(vfsPath);
        VFSStorageDescriptor contentAccumulator = initTreeFrom(
                ignoredEmptyTree,
                Paths.get(descriptorDirPath.toString(), "tmp")
        );
        VFSDirectory retTree = fromFS(dirPath, vfsPath, contentAccumulator);
        VFSStorageDescriptor retDescriptor = initTreeFrom(retTree, descriptorDirPath);
        for (VFSFile file : retTree.getAllSubFilesRecursive()) {
            String filePath = file.getPath();
            byte[] fileContent = contentAccumulator.readFileContent(filePath);
            retDescriptor.writeNewFileContentInTheEnd(filePath, fileContent);
        }
        contentAccumulator.clearStorage();
        return new VFS(retTree, retDescriptor);
    }

    private static VFSDirectory fromFS(Path dirPath, String vfsPath, VFSStorageDescriptor storage) throws IOException {
        VFSDirectory rootVfsDirectory = emptyWithPath(vfsPath);
        try (Stream<Path> subPaths = Files.list(dirPath)) {
            for (Path curPath : subPaths.collect(toList())) {
                File curFile = curPath.toFile();
                String curVfsPath = vfsPath + "/" + curFile.getName();
                if (Files.isDirectory(curPath)) {
                    VFSDirectory childVfsDirectory = fromFS(curPath, curVfsPath, storage);
                    rootVfsDirectory.getSubDirectories().add(childVfsDirectory);
                } else {
                    try {
                        byte[] fileContent = Files.readAllBytes(curPath);
                        VFSFile vfsFile = new VFSFile(curVfsPath);

                        storage.writeNewFileContentInTheEnd(vfsFile.getPath(), fileContent);

                        rootVfsDirectory.getSubFiles().add(vfsFile);
                    } catch (IOException ex) {
                        System.out.println("Can't load content from path: " +
                                curPath + ", file will be missed in FS, ex: " + ex.getMessage()
                        );
                    }
                }
            }
        }
        return rootVfsDirectory;
    }

    public static void toFS(VFS vfs, Path dirPath) {
        toFSHelper(vfs, Paths.get(dirPath.toString(), vfs.getRootVFSDirectory().getName()));
    }

    private static void toFSHelper(VFS vfs, Path rootDirPath) {
        try {
            Files.createDirectories(rootDirPath);
            for (VFSFile vfsSubFile : vfs.getRootVFSDirectory().getSubFiles()) {
                Path fsSubFilePath = Paths.get(rootDirPath + "/" + vfsSubFile.getName());
                try {
                    Files.write(fsSubFilePath, vfs.getVfsStorageDescriptor().readFileContent(vfsSubFile.getPath()));
                } catch (IOException ex) {
                    System.out.println("Can't write to/create file with path " + fsSubFilePath);
                }
            }
            for (VFSDirectory vfsSubDirectory : vfs.getRootVFSDirectory().getSubDirectories()) {
                Path fsSubDirPath = Paths.get(rootDirPath + "/" + vfsSubDirectory.getName());
                toFSHelper(vfs, fsSubDirPath);
            }
        } catch (IOException ex) {
            System.out.println("Can't create directory with path " + rootDirPath);
        }
    }

}
