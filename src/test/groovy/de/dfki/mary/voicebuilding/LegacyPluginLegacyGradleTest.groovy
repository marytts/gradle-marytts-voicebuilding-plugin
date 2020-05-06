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
    void 'Gradle v6-1 cannot apply plugin'() {
        gradle.withGradleVersion('6.1').buildAndFail()
    }

    @Test
    void 'Gradle v6-2 can apply plugin'() {
        if (JavaVersion.current() > JavaVersion.VERSION_13)
            throw new SkipException('Gradle v6.2 does not support Java versions higher than 13')
        gradle.withGradleVersion('6.2').build()
    }
}
