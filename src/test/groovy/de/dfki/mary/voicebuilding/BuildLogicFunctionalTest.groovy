package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def voiceName = 'cmu-slt'
    def voiceLocale = Locale.US

    @BeforeSuite
    void setup() {
        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        gradle = GradleRunner.create().withProjectDir(projectDir)
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

        apply plugin: 'de.dfki.mary.voicebuilding'

        voice {
            name = "$voiceName"
        }

        task testVoiceProps(group: 'Verification') << {
            assert voice.name == "$voiceName"
            assert voice.language == "$voiceLocale.language"
            assert voice.region == "$voiceLocale.country"
        }
        """
    }

    @Test
    void testBuild() {
        def result = gradle.build()
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testModel() {
        def result = gradle.withArguments('model').build()
        println result.standardOutput
        assert result.task(':model').outcome == SUCCESS
    }

    @Test
    void testVoiceProps() {
        def result = gradle.withArguments('testVoiceProps').build()
        println result.standardOutput
        assert result.task(':testVoiceProps').outcome == SUCCESS
    }
}
