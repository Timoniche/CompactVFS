package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.compactvfs.model.VFS;
import com.compactvfs.storage.VFSStorageDescriptor;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static com.compactvfs.model.VFSDirectory.VFS_PREFIX_PATH;
import static com.compactvfs.utils.DrawUtils.toTreeString;
import static junit.framework.TestCase.fail;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VFSStorageDescriptorTest {
    private final String BASE_PATH = System.getProperty("user.dir");

    @Test
    @Parameters(method = "storePathAndVfsProvider")
    public void test1_saveVfs(Path pathToStore, VFSDirectory vfsDirectory) {
        System.out.println(toTreeString(vfsDirectory));
        try {
            Files.createDirectories(pathToStore);
            VFSStorageDescriptor descriptor = VFSStorageDescriptor.save(vfsDirectory, pathToStore);
            System.out.println("---File content positions---");
            System.out.println(descriptor.getFileContentPosition());
        } catch (IOException ex) {
            System.out.println("Can't save vfs to path: " + pathToStore + " ex: " + ex.getMessage());
            fail();
        }
    }

    @Test
    @Parameters(method = "pathVfsProvider")
    public void test2_loadVfs(Path descriptorPath) {
        try {
            VFS vfs = VFSStorageDescriptor.load(descriptorPath);
            VFSDirectory rootVfsDir = vfs.getRootVFSDirectory();
            System.out.println(toTreeString(rootVfsDir));
            VFSStorageDescriptor vfsStorageDescriptor = vfs.getVfsStorageDescriptor();
            System.out.println(vfsStorageDescriptor.getFileContentPosition());
        } catch (IOException ex) {
            System.out.println("Can't load descriptor from path: " + descriptorPath + " ex: " + ex.getMessage());
            fail();
        }
    }

    @SuppressWarnings("unused")
    Object[][] storePathAndVfsProvider() throws IOException {
        VFSFile file1 = new VFSFile(
                VFS_PREFIX_PATH + "simpleFS2/file1.txt"
        );
        VFSFile file2 = new VFSFile(
                VFS_PREFIX_PATH + "simpleFS2/file2.txt"
        );
        byte[] file1Content = Files.readAllBytes(Paths.get(BASE_PATH, "/src/test/filesystems/simpleFS2/file1.txt"));
        byte[] file2Content = Files.readAllBytes(Paths.get(BASE_PATH, "/src/test/filesystems/simpleFS2/file2.txt"));
        file1.setContent(file1Content);
        file2.setContent(file2Content);
        VFSDirectory simpleVFS2 = new VFSDirectory(
                VFS_PREFIX_PATH + "simpleFS2",
                Set.of(),
                Set.of(file1, file2)
        );

        Path simpleFSPathToStore = Paths.get(BASE_PATH, "__storage/descriptors");


        return new Object[][]{
                {
                        simpleFSPathToStore,
                        simpleVFS2
                }
        };
    }


    @SuppressWarnings("unused")
    Object[][] pathVfsProvider() {
        Path simpleFSPathToStore = Paths.get(BASE_PATH, "__storage/descriptors");


        return new Object[][]{
                {
                        Paths.get(simpleFSPathToStore.toString(), "descriptor_simpleFS2.ser")
                }
        };
    }
}
