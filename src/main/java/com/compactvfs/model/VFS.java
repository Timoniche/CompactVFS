package com.compactvfs.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.compactvfs.storage.VFSStorageDescriptor;

import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.model.VFSFile.getParentDir;

public class VFS {
    private final VFSDirectory rootVFSDirectory;
    private final VFSStorageDescriptor vfsStorageDescriptor;

    private static final int READLOCK_TIMEOUT_MS = 1;
    private static final int WRITELOCK_TIMEOUT_MS = 1;

    public VFS(VFSDirectory rootVFSDirectory, VFSStorageDescriptor vfsStorageDescriptor) {
        this.rootVFSDirectory = rootVFSDirectory;
        this.vfsStorageDescriptor = vfsStorageDescriptor;
    }

    public VFSDirectory getRootVFSDirectory() {
        return rootVFSDirectory;
    }

    public void readLockFileAndParents(VFSFile vfsFile, List<VFSDirectory> dirsOnPath) throws IOException {

        for (int i = 0; i < dirsOnPath.size(); i++) {
            VFSDirectory vfsDirectoryFromRoot = dirsOnPath.get(i);
            try {
                boolean dirLocked = vfsDirectoryFromRoot.getLock().readLock().tryLock(
                        READLOCK_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
                if (!dirLocked) {
                    unlockReadParents(i - 1, dirsOnPath);
                    throw new IOException("Timeout read lock, vfsFile: " + vfsFile.getPath());
                }
            } catch (InterruptedException ex) {
                unlockReadParents(i - 1, dirsOnPath);
                throw new IOException("Interrupted read lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
            }

        }

        try {
            boolean fileLocked = vfsFile.getLock().readLock().tryLock(READLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!fileLocked) {
                unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
                throw new IOException("t" +
                        "Timeout read lock, vfsFile: " + vfsFile.getPath());
            }
        } catch (InterruptedException ex) {
            unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
            throw new IOException("Interrupted read lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
        }

    }

    public void writeLockFileAndParents(VFSFile vfsFile, List<VFSDirectory> dirsOnPath) throws IOException {
        for (int i = 0; i < dirsOnPath.size(); i++) {
            VFSDirectory vfsDirectoryFromRoot = dirsOnPath.get(i);
            try {
                boolean dirLocked = vfsDirectoryFromRoot.getLock().writeLock().tryLock(
                        WRITELOCK_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
                if (!dirLocked) {
                    unlockWriteParents(i - 1, dirsOnPath);
                    throw new IOException("Timeout write lock, vfsFile: " + vfsFile.getPath());
                }
            } catch (InterruptedException ex) {
                unlockWriteParents(i - 1, dirsOnPath);
                throw new IOException("Interrupted write lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
            }

        }

        try {
            boolean fileLocked = vfsFile.getLock().writeLock().tryLock(WRITELOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!fileLocked) {
                unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
                throw new IOException("Timeout write lock, vfsFile: " + vfsFile.getPath());
            }
        } catch (InterruptedException ex) {
            unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
            throw new IOException("Interrupted write lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
        }
    }

    public byte[] readBytesFrom(VFSFile vfsFile) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));
        readLockFileAndParents(vfsFile, dirsOnPath);

        try {
            return vfsStorageDescriptor.readFileContent(vfsFile.getPath());
        } finally {
            vfsFile.getLock().readLock().unlock();
            unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    public void unlockReadParents(int fromIndex, List<VFSDirectory> dirsOnPathFromRoot) {
        for (int j = fromIndex; j >= 0; j--) {
            dirsOnPathFromRoot.get(j).getLock().readLock().unlock();
        }
    }

    public void unlockWriteParents(int fromIndex, List<VFSDirectory> dirsOnPathFromRoot) {
        for (int j = fromIndex; j >= 0; j--) {
            dirsOnPathFromRoot.get(j).getLock().writeLock().unlock();
        }
    }

    public void writeBytesTo(VFSFile vfsFile, byte[] content) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));
        writeLockFileAndParents(vfsFile, dirsOnPath);
        try {
            vfsStorageDescriptor.writeNewFileContentInTheEnd(vfsFile.getPath(), content);
        } finally {
            vfsFile.getLock().writeLock().unlock();
            unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    public Map<String, Long> getFilesContentBytePositions() {
        return vfsStorageDescriptor.getFileContentPosition();
    }

    public VFSDirectory getDirByPath(String dirPath) {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(dirPath);
        return dirsOnPath.get(dirsOnPath.size() - 1);
    }

    public VFSFile getFileByPath(String filePath) {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(filePath));
        VFSDirectory parentDir = dirsOnPath.get(dirsOnPath.size() - 1);
        return parentDir.getSubFile(filePath);
    }

    public List<VFSDirectory> getDirsFromRootToDir(String endVfsDirPath) {
        List<String> dirNamesOnPath = Arrays.asList(endVfsDirPath.split("/"));
        int vfsPrefixOffset = VFS_PREFIX_PATH.split("/").length;

        List<VFSDirectory> dirsOnPath = new ArrayList<>();
        dirsOnPath.add(getRootVFSDirectory());

        List<String> dirNamesAfterRoot = dirNamesOnPath.subList(vfsPrefixOffset + 1, dirNamesOnPath.size());
        StringBuilder pathPrefix = new StringBuilder(VFS_PREFIX_PATH + getRootVFSDirectory().getName());
        for (String dirNameOnPath : dirNamesAfterRoot) {
            pathPrefix.append("/").append(dirNameOnPath);
            VFSDirectory dirOnPath = dirsOnPath.get(dirsOnPath.size() - 1).getSubDirectory(pathPrefix.toString());
            dirsOnPath.add(dirOnPath);
        }
        return dirsOnPath;
    }
}
