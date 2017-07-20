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
        def projectDir = new File("${System.properties.testProjectDir}/legacyPlugin")
        projectDir.mkdirs()

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
    void testTemplates() {
        def result = gradle.withArguments('templates').build()
        println result.output
        assert result.task(':templates').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testTemplates').build()
        println result.output
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':testTemplates').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testTemplates'])
    void testLegacyInit() {
        def result = gradle.withArguments('legacyInit').build()
        println result.output
        assert result.task(':lab').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':text').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':wav').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':legacyInit').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyInit').build()
        println result.output
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':testLegacyInit').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPraatPitchmarker() {
        def result = gradle.withArguments('legacyPraatPitchmarker').build()
        println result.output
        assert result.task(':legacyPraatPitchmarker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPraatPitchmarker').build()
        println result.output
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testLegacyPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCEPMaker() {
        def result = gradle.withArguments('legacyMCEPMaker').build()
        println result.output
        assert result.task(':legacyMCEPMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyMCEPMaker').build()
        println result.output
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCEPMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyPhoneUnitLabelComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyHalfPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyTranscriptionAligner() {
        def result = gradle.withArguments('legacyTranscriptionAligner').build()
        println result.output
        assert result.task(':generateAllophones').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':legacyTranscriptionAligner').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyTranscriptionAligner').build()
        println result.output
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':testLegacyTranscriptionAligner').outcome == SUCCESS
    }

    @Test
    void testFeatureLister() {
        def result = gradle.withArguments('featureLister').build()
        println result.output
        assert result.task(':featureLister').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testFeatureLister').build()
        println result.output
        assert result.task(':featureLister').outcome == UP_TO_DATE
        assert result.task(':testFeatureLister').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testFeatureLister'])
    void testPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('phoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':phoneUnitFeatureComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':phoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testFeatureLister'])
    void testHalfPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('halfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':halfPhoneUnitFeatureComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testHalfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':halfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testHalfPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyWaveTimelineMaker() {
        def result = gradle.withArguments('legacyWaveTimelineMaker').build()
        println result.output
        assert result.task(':legacyWaveTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyWaveTimelineMaker').build()
        println result.output
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyWaveTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyBasenameTimelineMaker() {
        def result = gradle.withArguments('legacyBasenameTimelineMaker').build()
        println result.output
        assert result.task(':legacyBasenameTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyBasenameTimelineMaker').build()
        println result.output
        assert result.task(':legacyBasenameTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyBasenameTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyMCEPMaker'])
    void testLegacyMCepTimelineMaker() {
        def result = gradle.withArguments('legacyMCepTimelineMaker').build()
        println result.output
        assert result.task(':legacyMCepTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyMCepTimelineMaker').build()
        println result.output
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCepTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitLabelComputer', 'testPhoneUnitFeatureComputer'])
    void testLegacyPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyPhoneLabelFeatureAligner').build()
        println result.output
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitLabelComputer', 'testHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyHalfPhoneLabelFeatureAligner').build()
        println result.output
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyPhoneUnitLabelComputer', 'testLegacyPhoneLabelFeatureAligner'])
    void testLegacyPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyPhoneUnitfileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyHalfPhoneUnitLabelComputer', 'testLegacyHalfPhoneLabelFeatureAligner'])
    void testLegacyHalfPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitfileWriter', 'testPhoneUnitFeatureComputer'])
    void testLegacyPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyPhoneFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyF0PolynomialFeatureFileWriter() {
        def result = gradle.withArguments('legacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0PolynomialFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyF0PolynomialFeatureFileWriter', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyAcousticFeatureFileWriter() {
        def result = gradle.withArguments('legacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyAcousticFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyAcousticFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyMCepTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyAcousticFeatureFileWriter'])
    void testLegacyJoinCostFileMaker() {
        def result = gradle.withArguments('legacyJoinCostFileMaker').build()
        println result.output
        assert result.task(':legacyJoinCostFileMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyJoinCostFileMaker').build()
        println result.output
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyJoinCostFileMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter'])
    void testLegacyCARTBuilder() {
        def result = gradle.withArguments('legacyCARTBuilder').build()
        println result.output
        assert result.task(':legacyCARTBuilder').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyCARTBuilder').build()
        println result.output
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':testLegacyCARTBuilder').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyDurationCARTTrainer() {
        def result = gradle.withArguments('legacyDurationCARTTrainer').build()
        println result.output
        assert result.task(':legacyDurationCARTTrainer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyDurationCARTTrainer').build()
        println result.output
        assert result.task(':legacyDurationCARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyDurationCARTTrainer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyF0CARTTrainer() {
        def result = gradle.withArguments('legacyF0CARTTrainer').build()
        println result.output
        assert result.task(':legacyF0CARTTrainer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyF0CARTTrainer').build()
        println result.output
        assert result.task(':legacyF0CARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0CARTTrainer').outcome == SUCCESS
    }

    @Test
    void testGenerateVoiceConfig() {
        def result = gradle.withArguments('generateVoiceConfig').build()
        println result.output
        assert result.task(':generateVoiceConfig').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateVoiceConfig').build()
        println result.output
        assert result.task(':generateVoiceConfig').outcome == UP_TO_DATE
        assert result.task(':testGenerateVoiceConfig').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyCARTBuilder', 'testGenerateVoiceConfig'])
    void testProcessResources() {
        def result = gradle.withArguments('processResources').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':processResources').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testProcessResources').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':testProcessResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyWaveTimelineMaker', 'testLegacyBasenameTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyPhoneFeatureFileWriter', 'testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyDurationCARTTrainer', 'testLegacyF0CARTTrainer'])
    void testProcessLegacyResources() {
        def result = gradle.withArguments('processLegacyResources').build()
        println result.output
        assert result.task(':processLegacyResources').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testProcessLegacyResources').build()
        println result.output
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':testProcessLegacyResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessResources', 'testProcessLegacyResources'])
    void testIntegrationTest() {
        def result = gradle.withArguments('integrationTest').build()
        println result.output
        assert result.task(':integrationTest').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testProcessResources'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.output
        assert result.task(':generatePom').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':generatePomProperties').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':jar').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testJar').build()
        println result.output
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        // TODO: jar *should* be up-to-date at this point, but the pom.properties file may cause it to rerun with Gradle 2.10+
        assert result.task(':jar').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':testJar').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testJar'])
    void testLegacyZip() {
        def result = gradle.withArguments('legacyZip').build()
        println result.output
        assert result.task(':legacyZip').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyZip').build()
        println result.output
        assert result.task(':legacyZip').outcome == UP_TO_DATE
        assert result.task(':testLegacyZip').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyZip'])
    void testLegacyDescriptor() {
        def result = gradle.withArguments('legacyDescriptor').build()
        println result.output
        assert result.task(':legacyDescriptor').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyDescriptor').build()
        println result.output
        assert result.task(':legacyDescriptor').outcome == UP_TO_DATE
        assert result.task(':testLegacyDescriptor').outcome == SUCCESS
    }
}
