import cn.jmix.baidufs.BaiduFileStorage
import cn.jmix.baidufs.BaiduFileStorageConfiguration
import io.jmix.core.CoreConfiguration
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.UuidProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import test_support.BaiduFileStorageTestConfiguration
import test_support.TestContextInititalizer

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ContextConfiguration(
        classes = [CoreConfiguration, BaiduFileStorageConfiguration,BaiduFileStorageTestConfiguration],
        initializers = [TestContextInititalizer]
)
class BaiduFileStorageTest extends Specification {

    @Autowired
    private FileStorage fileStorage

    def "save stream"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        def fileStream=this.getClass().getClassLoader().getResourceAsStream("files/simple.txt");
        def fileRef=fileStorage.saveStream(fileName,fileStream);
        def openedStream=fileStorage.openStream(fileRef);
        expect:
            openedStream!=null
    }


    def "fileExists"() {
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/09/6b63e503-c213-f324-d6df-a43fc66cefaf.txt"
        def fileName="6b63e503-c213-f324-d6df-a43fc66cefaf.txt"

        def fileref = new FileRef(storageName, fileKey, fileName);
        def exists = fileStorage.fileExists(fileref)

        expect:  exists

    }


    def "removeFile"(){
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/09/6b63e503-c213-f324-d6df-a43fc66cefaf.txt"
        def fileName="6b63e503-c213-f324-d6df-a43fc66cefaf.txt"

        def fileref = new FileRef(storageName, fileKey, fileName);
        fileStorage.removeFile(fileref)


        def exists = fileStorage.fileExists(fileref)

        expect:  !exists
    }


    def "Baidu storage initialized"() {
        expect:
        fileStorage.getStorageName() == BaiduFileStorage.DEFAULT_STORAGE_NAME
    }
}