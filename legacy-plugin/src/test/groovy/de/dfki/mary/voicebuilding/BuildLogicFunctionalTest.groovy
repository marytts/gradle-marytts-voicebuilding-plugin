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

        legacyPhoneUnitLabelComputer.dependsOn lab

        legacyHalfPhoneUnitLabelComputer.dependsOn lab

        generateAllophones.dependsOn legacyInit

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

        task testLegacyPhoneUnitLabelComputer(group: 'Verification') {
            dependsOn legacyPhoneUnitLabelComputer
            doLast {
                assert fileTree(buildDir).include('phonelab/*.lab').files
            }
        }

        task testLegacyHalfPhoneUnitLabelComputer(group: 'Verification') {
            dependsOn legacyHalfPhoneUnitLabelComputer
            doLast {
                assert fileTree(buildDir).include('halfphonelab/*.hplab').files
            }
        }

        task testLegacyTranscriptionAligner(group: 'Verification') {
            dependsOn legacyTranscriptionAligner
            doLast {
                assert fileTree(buildDir).include('allophones/*.xml').files
            }
        }

        task testLegacyFeatureLister(group: 'Verification') {
            dependsOn legacyFeatureLister
            doLast {
                assert file("\$buildDir/features.txt")
            }
        }

        task testLegacyPhoneUnitFeatureGenerator(group: 'Verification') {
            dependsOn legacyPhoneUnitFeatureGenerator
            doLast {
                assert fileTree(buildDir).include('phonefeatures/*.pfeats').files
            }
        }

        task testLegacyHalfPhoneUnitFeatureGenerator(group: 'Verification') {
            dependsOn legacyHalfPhoneUnitFeatureGenerator
            doLast {
                assert fileTree(buildDir).include('halfphonefeatures/*.hpfeats').files
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

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyHalfPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyHalfPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyTranscriptionAligner() {
        def result = gradle.withArguments('legacyTranscriptionAligner').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == SUCCESS
        assert result.task(':legacyTranscriptionAligner').outcome == SUCCESS
        result = gradle.withArguments('testLegacyTranscriptionAligner').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':testLegacyTranscriptionAligner').outcome == SUCCESS
    }

    @Test
    void testLegacyFeatureLister() {
        def result = gradle.withArguments('legacyFeatureLister').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == SUCCESS
        result = gradle.withArguments('testLegacyFeatureLister').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':testLegacyFeatureLister').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner'])
    void testLegacyPhoneUnitFeatureGenerator() {
        def result = gradle.withArguments('legacyPhoneUnitFeatureGenerator').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureGenerator').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPhoneUnitFeatureGenerator').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureGenerator').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitFeatureGenerator').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner'])
    void testLegacyHalfPhoneUnitFeatureGenerator() {
        def result = gradle.withArguments('legacyHalfPhoneUnitFeatureGenerator').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureGenerator').outcome == SUCCESS
        result = gradle.withArguments('testLegacyHalfPhoneUnitFeatureGenerator').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureGenerator').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitFeatureGenerator').outcome == SUCCESS
    }
}
