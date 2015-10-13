package de.dfki.mary.voicebuilding

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
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { new File(it) }

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath(pluginClasspath)

        // Add the logic under test to the test build
        buildFile << """
        plugins {
            id 'de.dfki.mary.voicebuilding-legacy'
        }

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-legacy')
        }

        task testTemplates(group: 'Verification') {
            dependsOn templates
            doLast {
                assert fileTree(buildDir).include('templates/*.java').files
                assert fileTree(buildDir).include('templates/*.config').files
            }
        }

        task testLegacyInit(group: 'Verification') {
            dependsOn legacyInit
            doLast {
                assert file("\$buildDir/database.config")
            }
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.build()
        println result.standardOutput
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.standardOutput
        assert result.task(':testPlugins').outcome == SUCCESS
    }

    @Test
    void testTemplates() {
        def result = gradle.withArguments('templates').build()
        println result.standardOutput
        assert result.task(':templates').outcome == SUCCESS
        result = gradle.withArguments('testTemplates').build()
        println result.standardOutput
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':testTemplates').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testTemplates'])
    void testLegacyInit() {
        def result = gradle.withArguments('legacyInit').build()
        println result.standardOutput
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == SUCCESS
        result = gradle.withArguments('testLegacyInit').build()
        println result.standardOutput
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':testLegacyInit').outcome == SUCCESS
    }
}
