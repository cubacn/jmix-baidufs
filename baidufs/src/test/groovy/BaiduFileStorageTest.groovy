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


    def "save open remove"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        String string = "Text for testing.";
        InputStream inputStream = new ByteArrayInputStream(string.getBytes());
        def fileRef=fileStorage.saveStream(fileName,inputStream);
        def fileExists= fileStorage.fileExists(fileRef)
        def openedStream=fileStorage.openStream(fileRef);
        def fileOpened =openedStream!=null
        fileStorage.removeFile(fileRef)
        expect:
        verifyAll {
            fileExists
            fileOpened
        }
    }

    def "baidu storage initialized"() {
        expect:
        fileStorage.getStorageName() == BaiduFileStorage.DEFAULT_STORAGE_NAME
    }


}