package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.BeforeSuite
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class DataPluginFunctionalTest {

    GradleRunner gradle
    final List DEFAULT_ARGS = ['--warning-mode', 'all', '--stacktrace']

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()

        gradle = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()

        // Add the logic under test to the test build
        new File(projectDir, 'gradle.properties').withWriter {
            def dataDependencyName = 'cmu_time_awb'
            it.println "dataDependencyName=$dataDependencyName"
            it.println "dataDependency=org.festvox:$dataDependencyName::ldom@tar.bz2"
            it.println "voiceGender=male"
        }
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('dataPluginFunctionalTestBuildScript.gradle')
        }
    }

    @DataProvider
    Object[][] taskNames() {
        // task name to run, and whether to chase it with a test task named "testName"
        [
                ['help', false],
                ['testConfigurations', false],
                ['testSourceSets', false],
                ['testDependencies', false],
                ['templates', true],
                ['wav', true],
                ['basenames', true],
                ['praatPitchExtractor', true],
                ['praatPitchmarker', true],
                ['pitchmarkConverter', true],
                ['mcepExtractor', true],
                ['generateAllophones', true],
                ['generatePhoneFeatures', true],
                ['generateHalfPhoneFeatures', true]
        ]
    }

    @Test(dataProvider = 'taskNames')
    void testTasks(String taskName, boolean runTestTask) {
        def result = gradle.withArguments(DEFAULT_ARGS + taskName).build()
        assert result.task(":$taskName").outcome in [SUCCESS, UP_TO_DATE]
        if (runTestTask) {
            def testTaskName = 'test' + taskName.capitalize()
            result = gradle.withArguments(DEFAULT_ARGS + testTaskName).build()
            assert result.task(":$taskName").outcome == UP_TO_DATE
            assert result.task(":$testTaskName").outcome == SUCCESS
        }
    }
}
