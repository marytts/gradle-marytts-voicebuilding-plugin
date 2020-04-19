package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BasePluginFunctionalTest {

    def gradle

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()

        gradle = GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().forwardOutput()

        // Add the logic under test to the test build
        new File(projectDir, 'gradle.properties').withWriter {
            it.println "group=de.dfki.mary"
            it.println "maryVersion=$System.properties.maryVersion"
            it.println "version=1.2.3"
            def voiceLocale = Locale.US
            def voiceGender = 'female'
            it.println "voiceDescription=A $voiceGender ${voiceLocale.getDisplayLanguage(Locale.ENGLISH)} unit selection voice"
            it.println "voiceGender=$voiceGender"
            it.println "voiceName=cmu-slt"
            it.println "voiceNameCamelCase=CmuSlt"
            it.println "voiceLicenseName=Arctic"
            it.println "voiceLicenseUrl=http://festvox.org/cmu_arctic/cmu_arctic/cmu_us_slt_arctic/COPYING"
            it.println "voiceLocaleLanguage=$voiceLocale.language"
            it.println "voiceLocaleRegion=$voiceLocale.country"
            it.println "voiceSamplingRate=16000"
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
                ['testGeneratePomFileForMavenJavaPublication', false],
                ['generatePomProperties', true],
                ['jar', true],
                ['test', false]
        ]
    }

    @Test(dataProvider = 'taskNames')
    void testTasks(String taskName, boolean runTestTask) {
        def result = gradle.withArguments(taskName).build()
        assert result.task(":$taskName").outcome in [SUCCESS, UP_TO_DATE]
        if (runTestTask) {
            def testTaskName = 'test' + taskName.capitalize()
            result = gradle.withArguments(testTaskName).build()
            assert result.task(":$taskName").outcome == UP_TO_DATE
            assert result.task(":$testTaskName").outcome == SUCCESS
        }
    }
}
