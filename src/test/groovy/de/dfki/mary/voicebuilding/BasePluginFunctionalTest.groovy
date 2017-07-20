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

    @DataProvider
    Object[][] taskNames() {
        // task name to run, and whether to chase it with a test task named "testName"
        [
                ['help', false],
                ['testPlugins', false],
                ['testVoiceProps', false],
                ['testJavaCompatibility', false],
                ['generateSource', true],
                ['compileTestJava', true],
                ['compileJava', true],
                ['compileIntegrationTestGroovy', true],
                ['generateVoiceConfig', true],
                ['generateServiceLoader', true],
                ['generatePom', true],
                ['generatePomProperties', true],
                ['jar', true],
                ['test', false]
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
