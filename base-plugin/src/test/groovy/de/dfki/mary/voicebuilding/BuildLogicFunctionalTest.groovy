package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def group = 'de.dfki.mary'
    def voiceName = 'cmu-slt'
    def voiceLocale = Locale.US
    def voiceDescription = "A female $voiceLocale.displayLanguage unit selection voice"
    def voiceLicenseName = 'Arctic'
    def voiceLicenseUrl = 'http://festvox.org/cmu_arctic/cmu_arctic/cmu_us_slt_arctic/COPYING'

    @BeforeSuite
    void setup() {
        def testKitDir = new File(System.properties.testKitDir)

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

        gradle = GradleRunner.create().withTestKitDir(testKitDir).withProjectDir(projectDir).withPluginClasspath(pluginClasspath)

        // Add the logic under test to the test build
        buildFile << """
        buildscript {
            repositories {
                jcenter()
            }
            dependencies {
                classpath group: 'xmlunit', name: 'xmlunit', version: '1.6'
            }
        }

        plugins {
            id 'de.dfki.mary.voicebuilding-base'
        }

        group "$group"

        voice {
            name = "$voiceName"
            license {
                name = "$voiceLicenseName"
                url = "$voiceLicenseUrl"
            }
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
            assert voice.description == "$voiceDescription"
            assert voice.license?.name == "$voiceLicenseName"
        }

        task testGenerateSource(group: 'Verification') {
            dependsOn generateSource
            doLast {
                assert file("\$buildDir/generatedSrc/main/java/marytts/voice/\$voice.nameCamelCase/Config.java").exists()
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

        task testGenerateVoiceConfig(group: 'Verification') {
            dependsOn generateVoiceConfig
            doLast {
                def configFile = file("\$buildDir/resources/main/marytts/voice/\$voice.nameCamelCase/voice.config")
                assert configFile.exists()
                assert configFile.withReader { it.readLine().contains(voice.name) }
            }
        }

        task testGenerateServiceLoader(group: 'Verification') {
            dependsOn generateServiceLoader
            doLast {
                def serviceLoaderFile = file("\$buildDir/resources/main/META-INF/services/marytts.config.MaryConfig")
                assert serviceLoaderFile.exists()
                assert serviceLoaderFile.text == "marytts.voice.\${voice.nameCamelCase}.Config"
            }
        }

        import org.custommonkey.xmlunit.XMLUnit

        task testGeneratePom(group: 'Verification') {
            dependsOn generatePom
            doLast {
                def pomFile = file("\$buildDir/resources/main/META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.xml")
                assert pomFile.exists()
                def pomXml = '''<?xml version="1.0"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>$group</groupId>
                      <artifactId>$projectDir.name</artifactId>
                      <version>unspecified</version>
                      <description>$voiceDescription</description>
                      <licenses>
                        <license>
                          <name>$voiceLicenseName</name>
                          <url>$voiceLicenseUrl</url>
                        </license>
                      </licenses>
                      <dependencies>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.12</version>
                          <scope>test</scope>
                        </dependency>
                        <dependency>
                          <groupId>de.dfki.mary</groupId>
                          <artifactId>marytts-runtime</artifactId>
                          <version>5.1.1</version>
                          <scope>compile</scope>
                        </dependency>
                      </dependencies>
                    </project>'''
                XMLUnit.ignoreWhitespace = true
                assert XMLUnit.compareXML(pomFile.text, pomXml).similar()
            }
        }

        task testGeneratePomProperties(group: 'Verification') {
            dependsOn generatePomProperties
            doLast {
                def pomPropertiesFile = file("\$buildDir/resources/main/META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.properties")
                assert pomPropertiesFile.exists()
                assert pomPropertiesFile.readLines() == ['version=unspecified', 'groupId=$group', 'artifactId=$projectDir.name']
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

    @Test
    void testGenerateVoiceConfig() {
        def result = gradle.withArguments('generateVoiceConfig').build()
        println result.standardOutput
        assert result.task(':generateVoiceConfig').outcome == SUCCESS
        result = gradle.withArguments('testGenerateVoiceConfig').build()
        println result.standardOutput
        assert result.task(':generateVoiceConfig').outcome == UP_TO_DATE
        assert result.task(':testGenerateVoiceConfig').outcome == SUCCESS
    }

    @Test
    void testGenerateServiceLoader() {
        def result = gradle.withArguments('generateServiceLoader').build()
        println result.standardOutput
        assert result.task(':generateServiceLoader').outcome == SUCCESS
        result = gradle.withArguments('testGenerateServiceLoader').build()
        println result.standardOutput
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':testGenerateServiceLoader').outcome == SUCCESS
    }

    @Test
    void testGeneratePom() {
        def result = gradle.withArguments('generatePom').build()
        println result.standardOutput
        assert result.task(':generatePom').outcome == SUCCESS
        result = gradle.withArguments('testGeneratePom').build()
        println result.standardOutput
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':testGeneratePom').outcome == SUCCESS
    }

    @Test
    void testGeneratePomProperties() {
        def result = gradle.withArguments('generatePomProperties').build()
        println result.standardOutput
        assert result.task(':generatePomProperties').outcome == SUCCESS
        result = gradle.withArguments('testGeneratePomProperties').build()
        println result.standardOutput
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        assert result.task(':testGeneratePomProperties').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testCompileTestJava'])
    void testTest() {
        def result = gradle.withArguments('test').build()
        println result.standardOutput
        assert result.task(':test').outcome == SUCCESS
    }
}
