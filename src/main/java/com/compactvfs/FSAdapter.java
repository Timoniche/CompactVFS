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

import static com.compactvfs.utils.DrawUtils.toTreeString;

public class FSAdapter {
    private static final String BASE_PATH = System.getProperty("user.dir");

    public static VFSDirectory fromFS(Path dirPath, String vfsPath) {
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
                            rootVfsDirectory.getSubFiles().add(
                                  new VFSFile(curVfsPath)
                            );
                        }
                    }
            );
        } catch (IOException ex) {
            System.out.println("Can't load FS due to " + ex.getMessage());
            return null;
        }
        return rootVfsDirectory;
    }

    public static void main(String[] args) {
        VFSDirectory vfs = fromFS(Paths.get(BASE_PATH + "/src"), "root/src");
        System.out.println(toTreeString(vfs));
    }
}
