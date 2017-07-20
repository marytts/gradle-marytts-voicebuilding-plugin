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

    @DataProvider
    Object[][] taskNames() {
        // task name to run, and whether to chase it with a test task named "testName"
        [
                ['help', false],
                ['testPlugins', false],
                ['testDataDependencies', false],
                ['processDataResources', true],
                ['wav', true],
                ['text', true],
                ['lab', true]
        ]
    }

    @Test(dataProvider = 'taskNames')
    void testTasks(String taskName, boolean runTestTask) {
        def result = gradle.withArguments(taskName).build()
        println result.output
        assert result.task(":$taskName").outcome in [SUCCESS, UP_TO_DATE]
        if (runTestTask) {
            def testTaskName = 'test' + taskName.capitalize()
            result = gradle.withArguments(testTaskName).build()
            println result.output
            assert result.task(":$taskName").outcome == UP_TO_DATE
            assert result.task(":$testTaskName").outcome == SUCCESS
        }
    }
}
