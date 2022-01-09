package storage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Set;

import com.compactvfs.model.VFS;
import com.compactvfs.storage.FSAdapter;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import com.compactvfs.storage.VFSStorageDescriptor;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static com.compactvfs.storage.FSAdapter.toFS;
import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.storage.VFSStorageDescriptor.initTreeFrom;
import static com.compactvfs.utils.DrawUtils.toTreeString;
import static junit.framework.TestCase.fail;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FSAdapterTest {
    private final String BASE_PATH = System.getProperty("user.dir");

    @Test
    @Parameters(method = "pathProvider")
    public void test1_fromFS(Path fsPath, Path descriptorDirPath) throws IOException {
        VFS vfs = FSAdapter.fromFS(fsPath, descriptorDirPath);
        System.out.println(toTreeString(vfs.getRootVFSDirectory()));
    }

    @Test
    @Parameters(method = "vfsProvider")
    public void test2_toFS(VFS vfs) {
        Path pathToStore = Paths.get(BASE_PATH, "src/test/decodedFilesystems");
        toFS(vfs, pathToStore);
        System.out.println("Decoded FS is stored at " + pathToStore);
    }

    @Test
    @Parameters(method = "pathProvider")
    public void test3_fromToFs(Path fsPath, Path descriptorDirPath) throws IOException {
        VFS vfs = FSAdapter.fromFS(fsPath, descriptorDirPath);
        System.out.println(toTreeString(vfs.getRootVFSDirectory()));
        Path pathToStore = Paths.get(BASE_PATH, "src/test/decodedFilesystems");
        toFS(vfs, pathToStore);
        System.out.println("Decoded FS is stored at " + pathToStore);
        try {
            verifyDirsAreEqual(fsPath, Paths.get(pathToStore + "/" + fsPath.getFileName()));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            fail();
        }
    }

    @SuppressWarnings("unused")
    Object[][] pathProvider() {
        Path simpleFS = Paths.get(BASE_PATH, "src/test/filesystems/simpleFS");
        Path descriptorPath = Paths.get(BASE_PATH, "/__storage/descriptors");

        return new Object[][]{
                {
                        simpleFS,
                        descriptorPath
                }
        };
    }

    @SuppressWarnings("unused")
    Object[][] vfsProvider() throws IOException {
        VFSFile file1 = new VFSFile(
                VFS_PREFIX_PATH + "simpleFS/file1.txt"
        );
        VFSDirectory simpleVFSDir = new VFSDirectory(
                VFS_PREFIX_PATH + "simpleFS",
                Set.of(),
                Set.of(file1)
        );

        VFSStorageDescriptor vfsStorageDescriptor = initTreeFrom(
                simpleVFSDir,
                Paths.get(BASE_PATH, "/__storage/descriptors")
        );
        byte[] file1Content = Files.readAllBytes(Paths.get(BASE_PATH, "/src/test/filesystems/simpleFS/file1.txt"));
        vfsStorageDescriptor.writeNewFileContentInTheEnd(file1.getPath(), file1Content);

        VFS simpleVFS = new VFS(simpleVFSDir, vfsStorageDescriptor);
        return new Object[][]{
                {
                        simpleVFS
                }
        };
    }

    private static void verifyDirsAreEqual(Path one, Path other) throws IOException {
        Files.walkFileTree(one, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);
                Path relativize = one.relativize(file);
                Path fileInOther = other.resolve(relativize);
                byte[] otherBytes = Files.readAllBytes(fileInOther);
                byte[] theseBytes = Files.readAllBytes(file);
                if (!Arrays.equals(otherBytes, theseBytes)) {
                    throw new IOException(file + " is not equal to " + fileInOther);
                }
                return result;
            }
        });
    }
}
