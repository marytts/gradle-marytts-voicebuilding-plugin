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
    void testUSFeatureLister() {
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
    void testUSEnglishFeatureListerMain() {
        def expected = getClass().getResourceAsStream("features.en_US.txt").readLines()
        def actualFile = new File(tmpDir, 'actual.txt')
        System.setProperty('locale', "${Locale.US}")
        log.info "locale = ${System.properties.locale}"
        System.setProperty('outputFile', actualFile.path)
        log.info "outputFile = ${System.properties.outputFile}"
        FeatureLister.main()
        def actual = actualFile.readLines()
        assert expected == actual
    }

    @Test
    void testGermanFeatureListerMain() {
        def expected = getClass().getResourceAsStream("features.de.txt").readLines()
        def actualFile = new File(tmpDir, 'actual.txt')
        System.setProperty('locale', "${Locale.GERMAN}")
        log.info "locale = ${System.properties.locale}"
        System.setProperty('outputFile', actualFile.path)
        log.info "outputFile = ${System.properties.outputFile}"
        FeatureLister.main()
        def actual = actualFile.readLines()
        assert expected == actual
    }
}
