package com.compactvfs.model;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class VFSDirectory implements Comparable<VFSDirectory> {
    private final Set<VFSDirectory> subDirectories;
    private final Set<VFSFile> subFiles;

    private String path;

    public VFSDirectory(String path, Set<VFSDirectory> subDirectories, Set<VFSFile> subFiles) {
        this.path = path;
        this.subDirectories = subDirectories;
        this.subFiles = subFiles;
    }

    public Set<VFSDirectory> getSubDirectories() {
        return subDirectories;
    }

    public Set<VFSFile> getSubFiles() {
        return subFiles;
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
        if (subFiles.size() != rhs.getSubFiles().size()) {
            return false;
        }
        if (subDirectories.size() != rhs.getSubDirectories().size()) {
            return false;
        }
        if (!subFiles.equals(rhs.getSubFiles())) {
            return false;
        }

        boolean ret = true;
        TreeSet<VFSDirectory> sortedDirectories = new TreeSet<>(subDirectories);
        TreeSet<VFSDirectory> rhsSortedDirectories = new TreeSet<>(rhs.getSubDirectories());
        Iterator<VFSDirectory> rhsIterator = rhsSortedDirectories.iterator();
        for (VFSDirectory subDirectory : sortedDirectories) {
            ret &= subDirectory.compareStructure(rhsIterator.next());
        }

        return ret;
    }

}
