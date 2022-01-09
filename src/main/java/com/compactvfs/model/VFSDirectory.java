package com.compactvfs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.stream.Collectors.toMap;

public class VFSDirectory implements Comparable<VFSDirectory> {
    public static final String VFS_PREFIX_PATH = "~/vfs/";

    private final Map<String, VFSDirectory> pathSubDirectoriesMap;
    private final Map<String, VFSFile> pathSubFilesMap;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private String path;

    public VFSDirectory(String path, Set<VFSDirectory> subDirectories, Set<VFSFile> subFiles) {
        this.path = path;
        this.pathSubDirectoriesMap = subDirectories.stream()
                .collect(toMap(VFSDirectory::getPath, dir -> dir, (a, b) -> b, HashMap::new));
        this.pathSubFilesMap = subFiles.stream()
                .collect(toMap(VFSFile::getPath, file -> file, (a, b) -> b, HashMap::new));
    }

    public static VFSDirectory emptyWithPath(String path) {
        return new VFSDirectory(
                path,
                new TreeSet<>(),
                new TreeSet<>()
        );
    }

    public VFSDirectory getSubDirectory(String dirPath) {
        return pathSubDirectoriesMap.get(dirPath);
    }

    public VFSFile getSubFile(String filePath) {
        return pathSubFilesMap.get(filePath);
    }

    public void addSubDirectory(VFSDirectory vfsDirectory) {
        pathSubDirectoriesMap.put(vfsDirectory.getPath(), vfsDirectory);
    }

    public void addSubFile(VFSFile vfsFile) {
        pathSubFilesMap.put(vfsFile.getPath(), vfsFile);
    }

    public Set<VFSDirectory> getSubDirectories() {
        return new TreeSet<>(pathSubDirectoriesMap.values());
    }

    public Set<VFSFile> getSubFiles() {
        return new TreeSet<>(pathSubFilesMap.values());
    }

    // todo: encapsulate in VFS
    public ReadWriteLock getLock() {
        return lock;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        int index = path.lastIndexOf('/');
        return path.substring(index + 1);
    }

    public List<VFSFile> getAllSubFilesRecursive() {
        List<VFSFile> allSubFiles = new ArrayList<>();
        Stack<VFSDirectory> dfsStack = new Stack<>();
        dfsStack.push(this);
        while (!dfsStack.isEmpty()) {
            VFSDirectory currentDir = dfsStack.pop();
            allSubFiles.addAll(currentDir.getSubFiles());
            for (VFSDirectory subDir : currentDir.getSubDirectories()) {
                dfsStack.push(subDir);
            }
        }
        return allSubFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VFSDirectory vfsDirectory = (VFSDirectory) o;
        return compareTo(vfsDirectory) == 0;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(VFSDirectory o) {
        return path.compareTo(o.getPath());
    }

    public boolean compareStructure(VFSDirectory rhs) {
        if (!equals(rhs)) {
            return false;
        }
        if (getSubFiles().size() != rhs.getSubFiles().size()) {
            return false;
        }
        if (getSubDirectories().size() != rhs.getSubDirectories().size()) {
            return false;
        }
        if (!getSubFiles().equals(rhs.getSubFiles())) {
            return false;
        }

        boolean ret = true;
        TreeSet<VFSDirectory> sortedDirectories = new TreeSet<>(getSubDirectories());
        TreeSet<VFSDirectory> rhsSortedDirectories = new TreeSet<>(rhs.getSubDirectories());
        Iterator<VFSDirectory> rhsIterator = rhsSortedDirectories.iterator();
        for (VFSDirectory subDirectory : sortedDirectories) {
            ret &= subDirectory.compareStructure(rhsIterator.next());
        }

        return ret;
    }

}
