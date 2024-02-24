package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class LegacyPluginOneShotFunctionalTest {

    GradleRunner gradle
    final List DEFAULT_ARGS = ['--warning-mode', 'all', '--stacktrace']

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()
        new File(projectDir, 'settings.gradle').createNewFile()

        gradle = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()

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
    }

    @Test
    void testOneShotBuild() {
        def result = gradle.withArguments(DEFAULT_ARGS + 'build').build()
        assert result.task(":build").outcome == SUCCESS
    }
}
