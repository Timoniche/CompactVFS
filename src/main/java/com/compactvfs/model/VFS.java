package com.compactvfs.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.compactvfs.storage.VFSInputStream;
import com.compactvfs.storage.VFSStorageDescriptor;

import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.model.VFSFile.getParentDir;

public class VFS {
    private final VFSDirectory rootVFSDirectory;
    private final VFSStorageDescriptor vfsStorageDescriptor;

    private static final int READLOCK_TIMEOUT_MS = 10;
    private static final int WRITELOCK_TIMEOUT_MS = 10;

    public VFS(VFSDirectory rootVFSDirectory, VFSStorageDescriptor vfsStorageDescriptor) {
        this.rootVFSDirectory = rootVFSDirectory;
        this.vfsStorageDescriptor = vfsStorageDescriptor;
    }

    public VFSDirectory getRootVFSDirectory() {
        return rootVFSDirectory;
    }

    public boolean readLockFileAndParents(VFSFile vfsFile, List<VFSDirectory> dirsOnPath) {

        for (int i = 0; i < dirsOnPath.size(); i++) {
            VFSDirectory vfsDirectoryFromRoot = dirsOnPath.get(i);
            try {
                boolean dirLocked = vfsDirectoryFromRoot.getLock().readLock().tryLock(
                        READLOCK_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
                if (!dirLocked) {
                    unlockReadParents(i - 1, dirsOnPath);
                    System.out.println("Timeout read lock, vfsFile: " + vfsFile.getPath());
                    return false;
                }
            } catch (InterruptedException ex) {
                unlockReadParents(i - 1, dirsOnPath);
                System.out.println("Interrupted read lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
                return false;
            }

        }

        try {
            boolean fileLocked = vfsFile.getLock().readLock().tryLock(READLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!fileLocked) {
                unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
                System.out.println("Timeout read lock, vfsFile: " + vfsFile.getPath());
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
            System.out.println("Interrupted read lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
            return false;
        }

    }

    public boolean writeLockFileAndParents(VFSFile vfsFile, List<VFSDirectory> dirsOnPath) {
        for (int i = 0; i < dirsOnPath.size(); i++) {
            VFSDirectory vfsDirectoryFromRoot = dirsOnPath.get(i);
            try {
                boolean dirLocked = vfsDirectoryFromRoot.getLock().writeLock().tryLock(
                        WRITELOCK_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
                if (!dirLocked) {
                    unlockWriteParents(i - 1, dirsOnPath);
                    System.out.println("Timeout write lock, vfsFile: " + vfsFile.getPath());
                    return false;
                }
            } catch (InterruptedException ex) {
                unlockWriteParents(i - 1, dirsOnPath);
                System.out.println("Interrupted write lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
                return false;
            }

        }

        try {
            boolean fileLocked = vfsFile.getLock().writeLock().tryLock(WRITELOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!fileLocked) {
                unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
                System.out.println("Timeout write lock, vfsFile: " + vfsFile.getPath());
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
            System.out.println("Interrupted write lock, vfsFile: " + vfsFile.getPath() + " ex: " + ex.getMessage());
            return false;
        }
    }

    public byte[] readBytesFrom(VFSFile vfsFile) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));

        boolean locked = readLockFileAndParents(vfsFile, dirsOnPath);
        if (!locked) {
            return null;
        }

        try (VFSInputStream vfsInputStream = vfsStorageDescriptor.readFileContent(vfsFile.getPath())) {
            return vfsInputStream.readAllBytes();
        } finally {
            vfsFile.getLock().readLock().unlock();
            unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    /**
     *
     * @param b size >= n
     * @return count of read bytes
     */
    public int readNBytesFrom(VFSFile vfsFile, byte[] b, int n) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));

        boolean locked = readLockFileAndParents(vfsFile, dirsOnPath);
        if (!locked) {
            return 0;
        }

        try (VFSInputStream vfsInputStream = vfsStorageDescriptor.readFileContent(vfsFile.getPath())) {
            return vfsInputStream.readNBytes(b, n);
        } finally {
            vfsFile.getLock().readLock().unlock();
            unlockReadParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    public byte[] readAllFileBatched(VFSFile vfsFile) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));

        boolean locked = readLockFileAndParents(vfsFile, dirsOnPath);
        if (!locked) {
            return null;
        }

        try (VFSInputStream vfsInputStream = vfsStorageDescriptor.readFileContent(vfsFile.getPath())) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int n = 1000;
            byte[] b = new byte[n];
            int bytesRead;
            do {
                bytesRead = vfsInputStream.readNBytes(b, n);
                outputStream.write(b, 0, bytesRead);
            } while (bytesRead == n);
            return outputStream.toByteArray();
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

    public boolean writeBytesToNewFile(VFSFile vfsFile, byte[] content) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));
        boolean locked = writeLockFileAndParents(vfsFile, dirsOnPath);
        if (!locked) {
            return false;
        }
        try {
            vfsStorageDescriptor.writeNewFileContentInTheEnd(vfsFile.getPath(), content);
            return true;
        } finally {
            vfsFile.getLock().writeLock().unlock();
            unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    public boolean writeBytesToTheEndOfFile(VFSFile vfsFile, byte[] content) throws IOException {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(vfsFile.getPath()));
        boolean locked = writeLockFileAndParents(vfsFile, dirsOnPath);
        if (!locked) {
            return false;
        }
        try {
            vfsStorageDescriptor.writeBytesToTheEndOfFile(vfsFile.getPath(), content);
            return true;
        } finally {
            vfsFile.getLock().writeLock().unlock();
            unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
        }
    }

    public boolean addFile(String filePath, byte[] content) {
        List<VFSDirectory> dirsOnPath = getDirsFromRootToDir(getParentDir(filePath));

        if (writeLockParents(dirsOnPath)) {
            try {
                VFSDirectory parentDir = dirsOnPath.get(dirsOnPath.size() - 1);
                parentDir.addSubFile(new VFSFile(filePath));
                vfsStorageDescriptor.rebuildDfsTree(rootVFSDirectory);
                vfsStorageDescriptor.writeNewFileContentInTheEnd(filePath, content);
                return true;
            } catch (IOException ex) {
                System.out.println("VFS is corrupted");
            } finally {
                unlockWriteParents(dirsOnPath.size() - 1, dirsOnPath);
            }
        }
        return false;

    }

    public Map<String, List<Long>> getFilesContentBytePositions() {
        return vfsStorageDescriptor.getFileContentChunkPositions();
    }

    public boolean writeLockParents(List<VFSDirectory> dirsOnPath) {
        for (int i = 0; i < dirsOnPath.size(); i++) {
            VFSDirectory vfsDirectoryFromRoot = dirsOnPath.get(i);
            try {
                boolean dirLocked = vfsDirectoryFromRoot.getLock().writeLock().tryLock(
                        WRITELOCK_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS
                );
                if (!dirLocked) {
                    unlockWriteParents(i - 1, dirsOnPath);
                    return false;
                }
            } catch (InterruptedException ex) {
                unlockWriteParents(i - 1, dirsOnPath);
                return false;
            }
        }
        return true;
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
