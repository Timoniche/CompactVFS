package com.compactvfs.model;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VFSFile implements Comparable<VFSFile> {
    private String path;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public VFSFile(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    //todo: encapsulate in VFS
    public ReadWriteLock getLock() {
        return lock;
    }

    @SuppressWarnings("unused")
    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        int index = path.lastIndexOf('/');
        return path.substring(index + 1);
    }

    public String getParentDir() {
        return getParentDir(path);
    }

    public static String getParentDir(String filePath) {
        int index = filePath.lastIndexOf('/');
        return filePath.substring(0, index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VFSFile vfsFile = (VFSFile) o;
        return compareTo(vfsFile) == 0;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int compareTo(VFSFile o) {
        return path.compareTo(o.getPath());
    }

    @Override
    public String toString() {
        return path;
    }
}
