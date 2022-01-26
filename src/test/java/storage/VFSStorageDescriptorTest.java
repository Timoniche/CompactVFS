package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.compactvfs.model.VFS;
import com.compactvfs.storage.FSAdapter;
import com.compactvfs.storage.VFSStorageDescriptor;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static com.compactvfs.utils.DrawUtils.toTreeString;
import static junit.framework.TestCase.fail;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VFSStorageDescriptorTest {
    private final String BASE_PATH = System.getProperty("user.dir");

    @Test
    @Parameters(method = "pathProvider")
    public void test1_saveVfs(Path fsPath, Path descriptorDirPath) {
        try {
            VFS vfs = FSAdapter.fromFS(fsPath, descriptorDirPath);
            System.out.println(toTreeString(vfs.getRootVFSDirectory()));
            System.out.println(vfs.getFilesContentBytePositions());
        } catch (IOException ex) {
            System.out.println("Can't load VFS from FS, fsPath: " + fsPath + " descriptorPath: " + descriptorDirPath +
                    " ex: " + ex.getMessage());
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
            System.out.println("---File content positions---");
            System.out.println(vfs.getFilesContentBytePositions());
            for (VFSFile vfsFile : rootVfsDir.getSubFiles()) {
                System.out.println(vfsFile);
                byte[] fileContent = vfs.readBytesFrom(vfsFile);
                if (fileContent != null) {
                    String content = new String(fileContent, StandardCharsets.UTF_8);
                    System.out.println(content);
                }
            }
        } catch (IOException ex) {
            System.out.println("Can't load descriptor from path: " + descriptorPath + " ex: " + ex.getMessage());
            fail();
        }
    }

    @SuppressWarnings("unused")
    Object[][] pathProvider() {
        Path simpleFS2 = Paths.get(BASE_PATH, "src/test/filesystems/simpleFS2");
        Path descriptorDirPath = Paths.get(BASE_PATH, "__storage/descriptors");

        return new Object[][]{
                {
                        simpleFS2,
                        descriptorDirPath
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
