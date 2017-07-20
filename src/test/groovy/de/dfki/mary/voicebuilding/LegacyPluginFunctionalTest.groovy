package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class LegacyPluginFunctionalTest {

    def gradle

    def maryVersion = System.properties.maryVersion
    def group = 'de.dfki.mary'
    def version = '1.2.3'
    def voiceName = 'cmu-time-awb'
    def voiceNameCamelCase = 'CmuTimeAwb'
    def voiceGender = 'male'
    def voiceLocale = Locale.UK
    def voiceLicenseUrl = 'http://mary.dfki.de/download/arctic-license.html'

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath()

        // Add the logic under test to the test build
        new File(projectDir, 'gradle.properties').withWriter {
            it.println "maryVersion=$maryVersion"
            it.println "voiceName=$voiceName"
            it.println "voiceNameCamelCase=$voiceNameCamelCase"
            it.println "voiceGender=$voiceGender"
            it.println "voiceLocaleLanguage=$voiceLocale.language"
            it.println "voiceLocaleRegion=$voiceLocale.country"
            it.println "voiceLicenseUrl=$voiceLicenseUrl"
            it.println "group=$group"
            it.println "version=$version"
        }
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('legacyPluginFunctionalTestBuildScript.gradle')
        }
    }

    @DataProvider
    Object[][] taskNames() {
        // task name to run, and whether to chase it with a test task named "testName"
        [
                ['help', false],
                ['testPlugins', false],
                ['templates', true],
                ['legacyInit', true],
                ['legacyPraatPitchmarker', true],
                ['legacyMCEPMaker', true],
                ['legacyPhoneUnitLabelComputer', true],
                ['legacyHalfPhoneUnitLabelComputer', true],
                ['legacyTranscriptionAligner', true],
                ['featureLister', true],
                ['phoneUnitFeatureComputer', true],
                ['halfPhoneUnitFeatureComputer', true],
                ['legacyWaveTimelineMaker', true],
                ['legacyBasenameTimelineMaker', true],
                ['legacyMCepTimelineMaker', true],
                ['legacyPhoneLabelFeatureAligner', false],
                ['legacyHalfPhoneLabelFeatureAligner', false],
                ['legacyPhoneUnitfileWriter', true],
                ['legacyHalfPhoneUnitfileWriter', true],
                ['legacyPhoneFeatureFileWriter', true],
                ['legacyHalfPhoneFeatureFileWriter', true],
                ['legacyF0PolynomialFeatureFileWriter', true],
                ['legacyAcousticFeatureFileWriter', true],
                ['legacyJoinCostFileMaker', true],
                ['legacyCARTBuilder', true],
                ['legacyDurationCARTTrainer', true],
                ['legacyF0CARTTrainer', true],
                ['generateVoiceConfig', true],
                ['processResources', true],
                ['processLegacyResources', true],
                ['integrationTest', false],
                ['jar', true],
                ['legacyZip', true],
                ['legacyDescriptor', true]
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
