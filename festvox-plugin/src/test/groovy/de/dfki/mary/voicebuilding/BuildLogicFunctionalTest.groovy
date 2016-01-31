package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    @BeforeSuite
    void setup() {
        def testKitDir = new File(System.properties.testKitDir)

        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { new File(it) }

        gradle = GradleRunner.create().withTestKitDir(testKitDir).withProjectDir(projectDir).withPluginClasspath(pluginClasspath)

        // Add the logic under test to the test build
        buildFile.text = """
        plugins {
            id 'de.dfki.mary.voicebuilding-festvox'
        }

        repositories {
            ivy {
                url 'http://festvox.org/examples'
                layout 'pattern', {
                    artifact '[module]_[classifier]/packed/[artifact].[ext]'
                }
            }
        }

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('java')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-base')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-data')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-festvox')
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', classifier: 'ldom', ext: 'tar.bz2'
        }

        task testDataDependencies(group: 'Verification') << {
            assert configurations.data.dependencies.find { it.name == 'cmu_time_awb' }
        }

        task testProcessDataResources {
            group 'Verification'
            dependsOn processDataResources
            doLast {
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.wav').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.lab').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.data').files
            }
        }

        task testWav {
            group 'Verification'
            dependsOn wav
            doLast {
                assert fileTree(buildDir).include('wav/*.wav').files
            }
        }

        text.srcFileName = 'time.data'

        task testText {
            group 'Verification'
            dependsOn text
            doLast {
                assert fileTree(buildDir).include('text/*.txt').files
            }
        }

        task testLab {
            group 'Verification'
            dependsOn lab
            doLast {
                assert fileTree(buildDir).include('lab/*.lab').files
            }
        }
        """
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
