package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BasePluginFunctionalTest {

    def gradle

    def maryVersion = System.properties.maryVersion
    def group = 'de.dfki.mary'
    def version = '1.2.3'
    def voiceName = 'cmu-slt'
    def voiceNameCamelCase = 'CmuSlt'
    def voiceGender = 'female'
    def voiceLocale = Locale.US
    def voiceDescription = "A $voiceGender ${voiceLocale.getDisplayLanguage(Locale.ENGLISH)} unit selection voice"
    def voiceLicenseName = 'Arctic'
    def voiceLicenseUrl = 'http://festvox.org/cmu_arctic/cmu_arctic/cmu_us_slt_arctic/COPYING'

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
            it.println "voiceDescription=$voiceDescription"
            it.println "voiceLicenseName=$voiceLicenseName"
            it.println "voiceLicenseUrl=$voiceLicenseUrl"
            it.println "group=$group"
            it.println "version=$version"
        }
        new File(projectDir, 'build.gradle').withWriter {
            it << this.class.getResourceAsStream('basePluginFunctionalTestBuildScript.gradle')
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
    void testVoiceProps() {
        def result = gradle.withArguments('testVoiceProps').build()
        println result.output
        assert result.task(':testVoiceProps').outcome == SUCCESS
    }

    @Test
    void testJavaCompatibility() {
        def result = gradle.withArguments('testJavaCompatibility').build()
        println result.output
        assert result.task(':testJavaCompatibility').outcome == SUCCESS
    }

    @Test
    void testGenerateSource() {
        def result = gradle.withArguments('generateSource').build()
        println result.output
        assert result.task(':generateSource').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateSource').build()
        println result.output
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':testGenerateSource').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateSource'])
    void testCompileJava() {
        def result = gradle.withArguments('compileJava').build()
        println result.output
        assert result.task(':compileJava').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testCompileJava').build()
        println result.output
        assert result.task(':compileJava').outcome == UP_TO_DATE
        assert result.task(':testCompileJava').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileJava'])
    void testCompileTestJava() {
        def result = gradle.withArguments('compileTestJava').build()
        println result.output
        assert result.task(':compileTestJava').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testCompileTestJava').build()
        println result.output
        assert result.task(':compileTestJava').outcome == UP_TO_DATE
        assert result.task(':testCompileTestJava').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileTestJava'])
    void testCompileIntegrationTestGroovy() {
        def result = gradle.withArguments('compileIntegrationTestGroovy').build()
        println result.output
        assert result.task(':compileIntegrationTestGroovy').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testCompileIntegrationTestGroovy').build()
        println result.output
        assert result.task(':compileIntegrationTestGroovy').outcome == UP_TO_DATE
        assert result.task(':testCompileIntegrationTestGroovy').outcome == SUCCESS
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

    @Test
    void testGenerateServiceLoader() {
        def result = gradle.withArguments('generateServiceLoader').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateServiceLoader').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':testGenerateServiceLoader').outcome == SUCCESS
    }

    @Test
    void testGeneratePom() {
        def result = gradle.withArguments('generatePom').build()
        println result.output
        assert result.task(':generatePom').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGeneratePom').build()
        println result.output
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':testGeneratePom').outcome == SUCCESS
    }

    @Test
    void testGeneratePomProperties() {
        def result = gradle.withArguments('generatePomProperties').build()
        println result.output
        assert result.task(':generatePomProperties').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGeneratePomProperties').build()
        println result.output
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        assert result.task(':testGeneratePomProperties').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileJava', 'testGeneratePom', 'testGeneratePomProperties'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.output
        assert result.task(':jar').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testJar').build()
        println result.output
        assert result.task(':jar').outcome == UP_TO_DATE
        assert result.task(':testJar').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileTestJava'])
    void testTest() {
        def result = gradle.withArguments('test').build()
        println result.output
        assert result.task(':test').outcome in [SUCCESS, UP_TO_DATE]
    }
}
