package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class DataPluginFunctionalTest {

    def gradle

    def dataDependencyName = 'cmu_time_awb'
    def dataDependency = "org.festvox:$dataDependencyName::ldom@tar.bz2"

    @BeforeSuite
    void setup() {
        def projectDir = new File("${System.properties.testProjectDir}/dataPlugin")
        projectDir.mkdirs()

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath()

        // Add the logic under test to the test build
        new File(projectDir, 'gradle.properties').withWriter {
            it.println "dataDependencyName=$dataDependencyName"
            it.println "dataDependency=$dataDependency"
        }
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('dataPluginFunctionalTestBuildScript.gradle')
        }
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.output
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testConfigurations() {
        def result = gradle.withArguments('testConfigurations').build()
        println result.output
        assert result.task(':testConfigurations').outcome == SUCCESS
    }

    @Test
    void testSourceSets() {
        def result = gradle.withArguments('testSourceSets').build()
        println result.output
        assert result.task(':testSourceSets').outcome == SUCCESS
    }

    @Test
    void testDependencies() {
        def result = gradle.withArguments('testDependencies').build()
        println result.output
        assert result.task(':testDependencies').outcome == SUCCESS
    }

    @Test
    void testPraatPitchmarker() {
        def result = gradle.withArguments('praatPitchmarker').build()
        println result.output
        assert result.task(':wav').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':praatPitchmarker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testPraatPitchmarker').build()
        println result.output
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':praatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testPraatPitchmarker'])
    void testMcepMaker() {
        def result = gradle.withArguments('mcepMaker').build()
        println result.output
        assert result.task(':mcepMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testMcepMaker').build()
        println result.output
        assert result.task(':mcepMaker').outcome == UP_TO_DATE
        assert result.task(':testMcepMaker').outcome == SUCCESS
    }

    @Test
    void testGenerateAllophones() {
        def result = gradle.withArguments('generateAllophones').build()
        println result.output
        assert result.task(':text').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':generateAllophones').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateAllophones').build()
        println result.output
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':testGenerateAllophones').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateAllophones'])
    void testGeneratePhoneFeatures() {
        def result = gradle.withArguments('generatePhoneFeatures').build()
        println result.output
        assert result.task(':generatePhoneFeatures').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGeneratePhoneFeatures').build()
        println result.output
        assert result.task(':generatePhoneFeatures').outcome == UP_TO_DATE
        assert result.task(':testGeneratePhoneFeatures').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateAllophones'])
    void testGenerateHalfPhoneFeatures() {
        def result = gradle.withArguments('generateHalfPhoneFeatures').build()
        println result.output
        assert result.task(':generateHalfPhoneFeatures').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateHalfPhoneFeatures').build()
        println result.output
        assert result.task(':generateHalfPhoneFeatures').outcome == UP_TO_DATE
        assert result.task(':testGenerateHalfPhoneFeatures').outcome == SUCCESS
    }
}
