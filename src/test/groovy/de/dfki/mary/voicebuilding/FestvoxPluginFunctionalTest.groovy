package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class FestvoxPluginFunctionalTest {

    def gradle

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath()

        // Add the logic under test to the test build
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('festvoxPluginFunctionalTestBuildScript.gradle')
        }
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.output
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.output
        assert result.task(':testPlugins').outcome == SUCCESS
    }

    @Test
    void testDependencies() {
        def result = gradle.withArguments('testDataDependencies').build()
        println result.output
        assert result.task(':testDataDependencies').outcome == SUCCESS
    }

    @Test
    void testProcessDataResources() {
        def result = gradle.withArguments('testProcessDataResources').build()
        println result.output
        assert result.task(':testProcessDataResources').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testProcessDataResources').build()
        println result.output
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':testProcessDataResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessDataResources'])
    void testWav() {
        def result = gradle.withArguments('wav').build()
        println result.output
        assert result.task(':wav').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testWav').build()
        println result.output
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':testWav').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessDataResources'])
    void testText() {
        def result = gradle.withArguments('text').build()
        println result.output
        assert result.task(':text').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testText').build()
        println result.output
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':testText').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessDataResources'])
    void testLab() {
        def result = gradle.withArguments('lab').build()
        println result.output
        assert result.task(':lab').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLab').build()
        println result.output
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':testLab').outcome == SUCCESS
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':testLab').outcome == SUCCESS
    }
}
