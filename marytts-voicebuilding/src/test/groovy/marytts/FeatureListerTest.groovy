package marytts

import groovy.util.logging.Log4j

import org.testng.Assert
import org.testng.annotations.*

@Log4j
class FeatureListerTest {

    def tmpDir

    @BeforeClass
    void setUp() {
        tmpDir = File.createTempDir()
        log.info "tmpDir = $tmpDir"
    }

    @Test
    void testUsFeatureLister() {
        def expected = getClass().getResourceAsStream("features.en_US.txt").readLines()
        def actual = new FeatureLister(Locale.US).listFeatures()
        Assert.assertEquals expected, actual
    }

    @Test
    void testGermanFeatureLister() {
        def expected = getClass().getResourceAsStream("features.de.txt").readLines()
        def actual = new FeatureLister(Locale.GERMAN).listFeatures()
        Assert.assertEquals expected, actual
    }

    @Test
    void testFeatureListerMain() {
        def expected = getClass().getResourceAsStream("features.de.txt").readLines()
        def actualFile = new File(tmpDir, 'actual.txt')
        System.setProperty('locale', "${Locale.GERMAN}")
        System.setProperty('outputFile', actualFile.path)
        FeatureLister.main()
        def actual = actualFile.readLines()
        assert expected == actual
    }
}
