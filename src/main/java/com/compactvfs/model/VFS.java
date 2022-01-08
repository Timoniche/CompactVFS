package com.compactvfs.model;

import com.compactvfs.storage.VFSStorageDescriptor;

public class VFS {
    private final VFSDirectory rootVFSDirectory;
    private final VFSStorageDescriptor vfsStorageDescriptor;

    public VFS(VFSDirectory rootVFSDirectory, VFSStorageDescriptor vfsStorageDescriptor) {
        this.rootVFSDirectory = rootVFSDirectory;
        this.vfsStorageDescriptor = vfsStorageDescriptor;
    }

    public VFSDirectory getRootVFSDirectory() {
        return rootVFSDirectory;
    }

    public VFSStorageDescriptor getVfsStorageDescriptor() {
        return vfsStorageDescriptor;
    }
}
