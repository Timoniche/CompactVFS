import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import com.compactvfs.VFSTreeDfsCompressor;
import com.compactvfs.model.VFSDirectory;
import com.compactvfs.model.VFSFile;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static com.compactvfs.utils.DrawUtils.toTreeString;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

@RunWith(JUnitParamsRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VFSTreeDfsCompressorTest {
    private final String BASE_PATH = System.getProperty("user.dir");
    private final List<String> TEST_FILE_NAMES = List.of("VFSTreeSimple.ser", "VFSTreeNested.ser");

    @Test
    @Parameters(method = "vfsProvider")
    public void test1_compress(VFSDirectory vfsDirectory, String outFileName) {
        try {
            String compressFilePath = BASE_PATH + "/storage/" + outFileName;
            File fileCompressTo = new File(compressFilePath);
            Files.createDirectories(fileCompressTo.getParentFile().toPath());
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileCompressTo, false)) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                    VFSTreeDfsCompressor.compress(objectOutputStream, vfsDirectory);
                }
            }
        } catch (IOException ex) {
            System.out.println("Compress ex occurred: " + ex.getMessage());
            fail();
        }
    }

    @Test
    @Parameters(method = "vfsNamesProvider")
    public void test2_decompress(String inputFileName) {
        try {
            String compressFilePath = BASE_PATH + "/storage/" + inputFileName;
            File fileCompressTo = new File(compressFilePath);
            try (FileInputStream fileInputStream = new FileInputStream(fileCompressTo)) {
                try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    VFSDirectory vfsDirectory = VFSTreeDfsCompressor.decompress(objectInputStream);
                    System.out.println(toTreeString(vfsDirectory));
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Decompress ex occurred: " + ex.getMessage());
            fail();
        }
    }

    @Test
    @Parameters(method = "vfsProvider")
    public void test3_compressDecompress(VFSDirectory vfsDirectory, String outFileName) {
        try {
            String compressFilePath = BASE_PATH + "/storage/" + outFileName;
            File fileCompressTo = new File(compressFilePath);
            Files.createDirectories(fileCompressTo.getParentFile().toPath());
            try (FileOutputStream fileOutputStream = new FileOutputStream(fileCompressTo, false)) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                    VFSTreeDfsCompressor.compress(objectOutputStream, vfsDirectory);
                }
            }
            try (FileInputStream fileInputStream = new FileInputStream(fileCompressTo)) {
                try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                    VFSDirectory vfsDirectoryBackup = VFSTreeDfsCompressor.decompress(objectInputStream);
                    System.out.println(toTreeString(vfsDirectory));

                    assertTrue(vfsDirectory.compareStructure(vfsDirectoryBackup));
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Compress-Decompress ex occurred: " + ex.getMessage());
            fail();
        }
    }

    @SuppressWarnings("unused")
    Object[][] vfsProvider() {
        VFSDirectory simpleDir = new VFSDirectory(
                "root/simpleDir",
                Set.of(),
                Set.of(
                        new VFSFile("root/simpleDir/file1"),
                        new VFSFile("root/simpleDir/file2"),
                        new VFSFile("root/simpleDir/file3")
                )
        );
        VFSDirectory nestedDir = new VFSDirectory(
                "root/nestedDir",
                Set.of(
                        new VFSDirectory(
                                "root/nestedDir/nestedA",
                                Set.of(),
                                Set.of(
                                        new VFSFile("root/nestedDir/nestedA/file11"),
                                        new VFSFile("root/nestedDir/nestedA/file12"),
                                        new VFSFile("root/nestedDir/nestedA/file13")
                                )
                        ),
                        new VFSDirectory(
                                "root/nestedDir/nestedB",
                                Set.of(),
                                Set.of(
                                        new VFSFile("root/nestedDir/nestedB/file21"),
                                        new VFSFile("root/nestedDir/nestedB/file22"),
                                        new VFSFile("root/nestedDir/nestedB/file23")
                                )
                        )
                ),
                Set.of(
                        new VFSFile("root/nestedDir/file01"),
                        new VFSFile("root/nestedDir/file02"),
                        new VFSFile("root/nestedDir/file03")
                )
        );
        return new Object[][]{
                {
                        simpleDir,
                        TEST_FILE_NAMES.get(0)
                },
                {
                        nestedDir,
                        TEST_FILE_NAMES.get(1)
                }
        };
    }

    @SuppressWarnings("unused")
    Object[][] vfsNamesProvider() {
        Object[][] ret = new Object[TEST_FILE_NAMES.size()][1];
        for (int i = 0; i < TEST_FILE_NAMES.size(); i++) {
            ret[i][0] = TEST_FILE_NAMES.get(i);
        }
        return ret;
    }

}