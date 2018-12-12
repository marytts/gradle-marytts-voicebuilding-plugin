package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.BeforeSuite
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class LegacyPluginFunctionalTest {

    def gradle

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().forwardOutput()

        // Add the logic under test to the test build
        new File(projectDir, 'gradle.properties').withWriter {
            it.println "group=de.dfki.mary"
            it.println "maryVersion=$System.properties.maryVersion"
            it.println "voiceGender=male"
            it.println "voiceLicenseUrl=http://mary.dfki.de/download/arctic-license.html"
            def voiceLocale = Locale.UK
            it.println "voiceLocaleLanguage=$voiceLocale.language"
            it.println "voiceLocaleRegion=$voiceLocale.country"
            it.println "voiceName=cmu-time-awb"
            it.println "voiceNameCamelCase=CmuTimeAwb"
            it.println "version=1.2.3"
        }
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('legacyPluginFunctionalTestBuildScript.gradle')
        }
        new File(projectDir, 'settings.gradle').withWriter {
            it << "enableFeaturePreview('STABLE_PUBLISHING')"
        }
    }

    @DataProvider
    Object[][] taskNames() {
        // task name to run, and whether to chase it with a test task named "testName"
        [
                ['help', false],
                ['testPlugins', false],
                ['processPhoneLabels', true],
                ['alignLabelsWithPrompts', true],
                ['splitPhoneLabelsIntoHalfPhones', true],
                ['phoneUnitFileMaker', true],
                ['halfPhoneUnitFileMaker', true],
                ['featureLister', true],
                ['phoneUnitFeatureComputer', true],
                ['generatePhoneFeatureDefinitionFile', true],
                ['halfPhoneUnitFeatureComputer', true],
                ['generateHalfPhoneFeatureDefinitionFile', true],
                ['makeBasenameDatagrams', true],
                ['basenameTimelineMaker', true],
                ['makeWaveDatagrams', true],
                ['waveTimelineMaker', true],
                ['makeMcepDatagrams', true],
                ['generateMcepTimelineHeader', true],
                ['mcepTimelineMaker', true],
                ['phoneFeatureFileMaker', true],
                ['halfPhoneFeatureFileMaker', true],
                ['f0ContourFeatureFileMaker', true],
                ['generateAcousticFeatureDefinitionFile', true],
                ['acousticFeatureFileMaker', true],
                ['generateJoinCostWeights', true],
                ['joinCostFileMaker', true],
                ['generateFeatureSequence', true],
                ['cartBuilder', true],
                ['generateDurationFeatureDescription', true],
                ['generateF0FeatureDescription', true],
                ['extractDurationFeatures', true],
                ['extractF0Features', true],
                ['trainDurationCart', true],
                ['trainF0LeftCart', true],
                ['trainF0MidCart', true],
                ['trainF0RightCart', true],
                ['convertDurationCart', true],
                ['convertF0LeftCart', true],
                ['convertF0MidCart', true],
                ['convertF0RightCart', true],
                ['generateVoiceConfig', true],
                ['processResources', true],
                ['processLegacyResources', true],
                ['integrationTest', false],
                ['jar', true],
                ['legacyZip', true],
                ['legacyDescriptor', true],
                ['testArtifacts', false]
        ]
    }

    @Test(dataProvider = 'taskNames')
    void testTasks(String taskName, boolean runTestTask) {
        def result = gradle.withArguments(taskName, '-s').build()
        assert result.task(":$taskName").outcome in [SUCCESS, UP_TO_DATE]
        if (runTestTask) {
            def testTaskName = 'test' + taskName.capitalize()
            result = gradle.withArguments(testTaskName).build()
            assert result.task(":$taskName").outcome == UP_TO_DATE
            assert result.task(":$testTaskName").outcome == SUCCESS
        }
    }
}
