package com.compactvfs.model;

public class VFSFile implements Comparable<VFSFile> {
    private String path;
    private byte[] content;

    public VFSFile(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
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
