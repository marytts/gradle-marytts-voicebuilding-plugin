package de.dfki.mary.voicebuilding

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.testng.SkipException
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

class LegacyPluginLegacyGradleTest {

    GradleRunner gradle

    @BeforeSuite
    void setup() {
        def projectDir = File.createTempDir()
        new File(projectDir, 'settings.gradle').createNewFile()
        gradle = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
        new File(projectDir, 'build.gradle').withWriter { buildScript ->
            buildScript.println 'plugins {'
            buildScript.println "  id 'de.dfki.mary.voicebuilding-legacy'"
            buildScript.println '}'
        }
    }

    @Test
    void 'Gradle v6-9 cannot apply plugin'() {
        gradle.withGradleVersion('6.9').buildAndFail()
    }

    @Test
    void 'Gradle v7-0 can apply plugin'() {
        if (JavaVersion.current() > JavaVersion.VERSION_16)
            throw new SkipException('Gradle v7.0 does not support Java versions higher than 16')
        gradle.withGradleVersion('7.0').build()
    }
}
