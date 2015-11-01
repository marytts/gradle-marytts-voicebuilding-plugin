package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def voiceName = 'cmu-slt'
    def voiceLocale = Locale.US

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
            id 'de.dfki.mary.voicebuilding-base'
        }

        voice {
            name = "$voiceName"
        }

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('java')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-base')
        }

        task testVoiceProps(group: 'Verification') << {
            assert voice.name == "$voiceName"
            assert voice.language == "$voiceLocale.language"
            assert voice.region == "$voiceLocale.country"
            assert voice.nameCamelCase == 'CmuSlt'
            assert voice.locale == new Locale("$voiceLocale.language", "$voiceLocale.country")
            assert voice.localeXml == "${voiceLocale.toLanguageTag()}"
        }

        task testGenerateSource(group: 'Verification') {
            dependsOn generateSource
            doLast {
                assert file("\$buildDir/generatedSrc/main/java/marytts/voice/\$voice.nameCamelCase/Config.java")
            }
        }

        task testCompileJava(group: 'Verification') {
            dependsOn compileJava
            doLast {
                assert file("\$buildDir/classes/main/marytts/voice/\$voice.nameCamelCase/Config.class").exists()
            }
        }

        task testCompileTestJava(group: 'Verification') {
            dependsOn compileTestJava
            doLast {
                assert file("\$buildDir/classes/test/marytts/voice/\$voice.nameCamelCase/ConfigTest.class").exists()
            }
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
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
    void testVoiceProps() {
        def result = gradle.withArguments('testVoiceProps').build()
        println result.standardOutput
        assert result.task(':testVoiceProps').outcome == SUCCESS
    }

    @Test
    void testGenerateSource() {
        def result = gradle.withArguments('generateSource').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == SUCCESS
        result = gradle.withArguments('testGenerateSource').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':testGenerateSource').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testGenerateSource'])
    void testCompileJava() {
        def result = gradle.withArguments('compileJava').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':compileJava').outcome == SUCCESS
        result = gradle.withArguments('testCompileJava').build()
        println result.standardOutput
        assert result.task(':compileJava').outcome == UP_TO_DATE
        assert result.task(':testCompileJava').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileJava'])
    void testCompileTestJava() {
        def result = gradle.withArguments('compileTestJava').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':compileJava').outcome == UP_TO_DATE
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':classes').outcome == UP_TO_DATE
        assert result.task(':compileTestJava').outcome == SUCCESS
        result = gradle.withArguments('testCompileTestJava').build()
        println result.standardOutput
        assert result.task(':compileTestJava').outcome == UP_TO_DATE
        assert result.task(':testCompileTestJava').outcome == SUCCESS
    }
}
