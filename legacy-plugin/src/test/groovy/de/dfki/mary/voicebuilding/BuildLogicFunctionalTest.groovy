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
            id 'de.dfki.mary.voicebuilding-festvox'
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', classifier: 'ldom'
        }

        text.srcFileName = 'time.data'

        legacyInit.dependsOn wav, text, lab

        legacyPraatPitchmarker.dependsOn legacyInit

        legacyMCEPMaker.dependsOn legacyInit

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-legacy')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-base')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-data')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-festvox')
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

        task testLegacyPraatPitchmarker(group: 'Verification') {
            dependsOn legacyPraatPitchmarker
            doLast {
                assert fileTree(buildDir).include('pm/*.PointProcess').files
                assert fileTree(buildDir).include('pm/*.pm').files
            }
        }

        task testLegacyMCEPMaker(group: 'Verification') {
            dependsOn legacyMCEPMaker
            doLast {
                assert fileTree(buildDir).include('mcep/*.mcep').files
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
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == SUCCESS
        assert result.task(':text').outcome == SUCCESS
        assert result.task(':lab').outcome == SUCCESS
        assert result.task(':legacyInit').outcome == SUCCESS
        result = gradle.withArguments('testLegacyInit').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':testLegacyInit').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPraatPitchmarker() {
        def result = gradle.withArguments('legacyPraatPitchmarker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPraatPitchmarker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testLegacyPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCEPMaker() {
        def result = gradle.withArguments('legacyMCEPMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyMCEPMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCEPMaker').outcome == SUCCESS
    }
}
