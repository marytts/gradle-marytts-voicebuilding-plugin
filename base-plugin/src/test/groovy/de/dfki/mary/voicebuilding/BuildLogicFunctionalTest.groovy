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
        buildFile.text = """
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

        task testJavaCompatibility(group: 'Verification') << {
            assert "\$sourceCompatibility" == '1.7'
            assert "\$targetCompatibility" == '1.7'
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

        task testCompileIntegrationTestGroovy(group: 'Verification') {
            dependsOn compileIntegrationTestGroovy
            doLast {
                assert file("\$buildDir/classes/integrationTest/marytts/voice/\$voice.nameCamelCase/LoadVoiceIT.class").exists()
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
        import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier

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
                def diff = XMLUnit.compareXML(pomFile.text, pomXml)
                diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier())
                assert diff.similar()
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

        import java.util.zip.ZipFile

        task testJar(group: 'Verification') {
            dependsOn jar
            doLast {
                def actual = new ZipFile(jar.archivePath).entries().findAll { !it.isDirectory() }.collect { it.name } as Set
                def expected = [
                    'META-INF/MANIFEST.MF',
                    'META-INF/services/marytts.config.MaryConfig',
                    "META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.xml",
                    "META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.properties",
                    "marytts/voice/\$voice.nameCamelCase/Config.class",
                    "marytts/voice/\$voice.nameCamelCase/voice.config"
                ] as Set
                assert actual == expected
            }
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':help']
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testPlugins']
    }

    @Test
    void testVoiceProps() {
        def result = gradle.withArguments('testVoiceProps').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testVoiceProps']
    }

    @Test
    void testJavaCompatibility() {
        def result = gradle.withArguments('testJavaCompatibility').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testJavaCompatibility']
    }

    @Test
    void testGenerateSource() {
        def result = gradle.withArguments('generateSource').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateSource']
        result = gradle.withArguments('testGenerateSource').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGenerateSource']
    }

    @Test(dependsOnMethods = ['testGenerateSource'])
    void testCompileJava() {
        def result = gradle.withArguments('compileJava').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':compileJava']
        result = gradle.withArguments('testCompileJava').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testCompileJava']
    }

    @Test(dependsOnMethods = ['testCompileJava'])
    void testCompileTestJava() {
        def result = gradle.withArguments('compileTestJava').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':compileTestJava']
        result = gradle.withArguments('testCompileTestJava').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testCompileTestJava']
    }

    @Test(dependsOnMethods = ['testCompileTestJava'])
    void testCompileIntegrationTestGroovy() {
        def result = gradle.withArguments('compileIntegrationTestGroovy').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':compileIntegrationTestGroovy']
        result = gradle.withArguments('testCompileIntegrationTestGroovy').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testCompileIntegrationTestGroovy']
    }

    @Test
    void testGenerateVoiceConfig() {
        def result = gradle.withArguments('generateVoiceConfig').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateVoiceConfig']
        result = gradle.withArguments('testGenerateVoiceConfig').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGenerateVoiceConfig']
    }

    @Test
    void testGenerateServiceLoader() {
        def result = gradle.withArguments('generateServiceLoader').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateServiceLoader']
        result = gradle.withArguments('testGenerateServiceLoader').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGenerateServiceLoader']
    }

    @Test
    void testGeneratePom() {
        def result = gradle.withArguments('generatePom').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generatePom']
        result = gradle.withArguments('testGeneratePom').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGeneratePom']
    }

    @Test
    void testGeneratePomProperties() {
        def result = gradle.withArguments('generatePomProperties').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generatePomProperties']
        result = gradle.withArguments('testGeneratePomProperties').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGeneratePomProperties']
    }

    @Test(dependsOnMethods = ['testCompileJava', 'testGeneratePom', 'testGeneratePomProperties'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':jar']
        result = gradle.withArguments('testJar').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testJar']
    }

    @Test(dependsOnMethods = ['testCompileTestJava'])
    void testTest() {
        def result = gradle.withArguments('test').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':test']
    }
}
