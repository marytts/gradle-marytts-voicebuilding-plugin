package de.dfki.mary.voicebuilding.data

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    @BeforeSuite
    void setup() {
        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        gradle = GradleRunner.create().withProjectDir(projectDir)
        if (System.properties.offline.toBoolean()) {
            gradle = gradle.withArguments('--offline')
        }
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        // Add the logic under test to the test build
        buildFile << """
        buildscript {
            dependencies {
                classpath files($pluginClasspath)
            }
        }

        apply plugin: 'de.dfki.mary.voicebuilding-data'
        """
    }

    @Test
    void testBuild() {
        def result = gradle.build()
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testConfigurations() {
        buildFile << """
        task testConfigurations << {
            assert configurations.data
        }
        """
        def result = gradle.withArguments('testConfigurations').build()
        assert result.task(':testConfigurations').outcome == SUCCESS
    }

    @Test
    void testSourceSets() {
        buildFile << """
        task testSourceSets << {
            assert sourceSets.data
        }
        """
        def result = gradle.withArguments('testSourceSets').build()
        assert result.task(':testSourceSets').outcome == SUCCESS
    }
}
