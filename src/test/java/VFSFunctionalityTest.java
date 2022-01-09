import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.compactvfs.model.VFS;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import com.compactvfs.storage.FSAdapter;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.utils.DrawUtils.toTreeString;
import static java.util.concurrent.CompletableFuture.runAsync;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VFSFunctionalityTest {
    private final String BASE_PATH = System.getProperty("user.dir");

    @Test
    @Parameters(method = "pathProvider")
    public void test1_findDirAndFile(Path fsPath, Path descriptorDirPath) throws IOException {
        VFS vfs = FSAdapter.fromFS(fsPath, descriptorDirPath);
        VFSDirectory simpleFS2 = vfs.getDirByPath(VFS_PREFIX_PATH + "nestedFS/simpleFS2");
        System.out.println(toTreeString(simpleFS2));

        assertEquals("~/vfs/nestedFS/simpleFS2", simpleFS2.getPath());

        VFSFile simpleFSFile1 = vfs.getFileByPath(VFS_PREFIX_PATH + "nestedFS/simpleFS/file1.txt");
        System.out.println(simpleFSFile1.getPath());

        assertEquals("~/vfs/nestedFS/simpleFS/file1.txt", simpleFSFile1.getPath());
    }

    @Test
    @Parameters(method = "pathProvider")
    public void test2_readAndWrites(Path fsPath, Path descriptorDirPath) throws IOException {
        VFS vfs = FSAdapter.fromFS(fsPath, descriptorDirPath);
        System.out.println("---FS---");
        System.out.println(toTreeString(vfs.getRootVFSDirectory()));

        VFSFile simpleFSFile1 = vfs.getFileByPath(VFS_PREFIX_PATH + "nestedFS/simpleFS/file1.txt");
        VFSFile simpleFS2File2 = vfs.getFileByPath(VFS_PREFIX_PATH + "nestedFS/simpleFS2/file2.txt");

        byte[] content1 = "content1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "content2".getBytes(StandardCharsets.UTF_8);

        Runnable readFile1 = () -> {
            try {
                System.out.println("reading file1");
                vfs.readBytesFrom(simpleFSFile1);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        };
        Runnable writeFile1 = () -> {
            try {
                System.out.println("writing file1");
                vfs.writeBytesTo(simpleFSFile1, content1);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        };
        Runnable readFile2 = () -> {
            try {
                System.out.println("reading file2");
                vfs.readBytesFrom(simpleFS2File2);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        };
        Runnable writeFile2 = () -> {
            try {
                System.out.println("writing file2");
                vfs.writeBytesTo(simpleFS2File2, content2);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        };

        final int THREADS = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            CompletableFuture<?> future1 = CompletableFuture.allOf(
                    Stream.generate(() -> runAsync(readFile1))
                    .limit(THREADS)
                    .toArray(CompletableFuture[]::new)
            );
            CompletableFuture<?> future2 = CompletableFuture.allOf(
                    Stream.generate(() -> runAsync(writeFile1))
                            .limit(THREADS)
                            .toArray(CompletableFuture[]::new)
            );
            CompletableFuture<?> future3 = CompletableFuture.allOf(
                    Stream.generate(() -> runAsync(readFile2))
                            .limit(THREADS)
                            .toArray(CompletableFuture[]::new)
            );
            CompletableFuture<?> future4 = CompletableFuture.allOf(
                    Stream.generate(() -> runAsync(writeFile2))
                            .limit(THREADS)
                            .toArray(CompletableFuture[]::new)
            );
            future1.join();
            future2.join();
            future3.join();
            future4.join();
        }
    }

    @SuppressWarnings("unused")
    Object[][] pathProvider() {
        Path simpleFS = Paths.get(BASE_PATH, "src/test/filesystems/nestedFS");
        Path descriptorPath = Paths.get(BASE_PATH, "/__storage/descriptors");

        return new Object[][]{
                {
                        simpleFS,
                        descriptorPath
                }
        };
    }
}
