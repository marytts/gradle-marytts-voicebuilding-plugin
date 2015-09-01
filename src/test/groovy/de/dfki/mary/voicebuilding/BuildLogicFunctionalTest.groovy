package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import java.nio.file.Files

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle

    @BeforeSuite
    void setup() {
        def projectDir = Files.createTempDirectory(null).toFile()
        gradle = GradleRunner.create().withProjectDir(projectDir)
    }

    @Test
    void testBuild() {
        def result = gradle.build()
        assert result.task(':help').outcome == SUCCESS
    }
}
