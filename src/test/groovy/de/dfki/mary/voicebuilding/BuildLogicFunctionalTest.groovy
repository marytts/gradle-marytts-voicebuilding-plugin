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

        apply plugin: 'de.dfki.mary.voicebuilding'

        repositories {
            ivy {
                url 'http://festvox.org/examples'
                layout 'pattern', {
                    artifact '[module]_[classifier]/packed/[artifact].[ext]'
                }
            }
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', version: '1.0', classifier: 'ldom', ext: 'tar.bz2'
        }

        voice {
            name = "$voiceName"
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
        buildFile << """
        task testVoiceProps << {
            assert voice.name == "$voiceName"
            assert voice.language == "$voiceLocale.language"
            assert voice.region == "$voiceLocale.country"
        }
        """
        def result = gradle.withArguments('testVoiceProps').build()
        println result.standardOutput
        assert result.task(':testVoiceProps').outcome == SUCCESS
    }
}
