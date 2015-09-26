package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def dataDependencyName = 'cmu_time_awb'
    def dataDependency = "org.festvox:$dataDependencyName::ldom@tar.bz2"

    @BeforeSuite
    void setup() {
        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        gradle = GradleRunner.create().withProjectDir(projectDir)
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        // Add the logic under test to the test build
        buildFile << """
        buildscript {
            dependencies {
                classpath files($pluginClasspath)
            }
        }

        apply plugin: 'de.dfki.mary.voicebuilding-data'

        repositories {
            flatDir dirs: "$projectDir.parent/testKitGradleHome"
        }

        dependencies {
            data "$dataDependency"
            runtime group: 'de.dfki.mary', name: 'marytts-common', version: '5.1.1'
        }

        task('text', type: FestvoxTextTask) {
            dependsOn processDataResources
            srcFile = file("\$sourceSets.data.output.resourcesDir/time.data")
            destDir = file("\$buildDir/text")
            generateAllophones.dependsOn it
        }

        task testConfigurations(group: 'Verification') << {
            assert configurations.data
        }

        task testSourceSets(group: 'Verification') << {
            assert sourceSets.data
        }

        task testDependencies(group: 'Verification') << {
            assert configurations.data.dependencies.find { it.name == "$dataDependencyName" }
        }

        processDataResources {
            from configurations.data
            filesMatching '*.tar.bz2', { tarFileDetails ->
                copy {
                    from tarTree(tarFileDetails.file)
                    into destinationDir
                    include '**/wav/*.wav', '**/lab/*.lab', '**/etc/*.data'
                    eachFile {
                        it.path = it.name
                    }
                    includeEmptyDirs = false
                }
                tarFileDetails.exclude()
            }
        }

        task testProcessDataResources {
            group 'Verification'
            dependsOn processDataResources
            doLast {
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.wav').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.lab').files
                assert fileTree(sourceSets.data.output.resourcesDir).include('*.data').files
            }
        }

        task testWav {
            group 'Verification'
            dependsOn wav
            doLast {
                assert fileTree("\$buildDir/wav").include('*.wav').files
            }
        }

        task testPraatPitchmarker {
            group 'Verification'
            dependsOn praatPitchmarker
            doLast {
                assert fileTree("\$buildDir/pm").include('*.PointProcess').files
                assert fileTree("\$buildDir/pm").include('*.pm').files
            }
        }

        task testText {
            group 'Verification'
            dependsOn text
            doLast {
                assert fileTree(buildDir).include('text/*.txt').files
            }
        }

        task testGenerateAllophones {
            group 'Verification'
            dependsOn generateAllophones
            doLast {
                assert fileTree("\$buildDir/prompt_allophones").include('*.xml').files
            }
        }

        task testMaryJavaExec(type: JavaExec) {
            classpath sourceSets.main.runtimeClasspath
            main 'marytts.util.PrintSystemProperties'
        }

        class FestvoxTextTask extends DefaultTask {
            @Input
            File srcFile

            @OutputDirectory
            File destDir

            @TaskAction
            void extract() {
                srcFile.eachLine { line ->
                    def m = line =~ /\\( (?<utt>.+) "(?<text>.+)" \\)/
                    if (m.matches()) {
                        new File("\$destDir/\${m.group('utt')}.txt").text = m.group('text')
                    }
                }
            }
        }
        """
    }

    @Test
    void testBuild() {
        def result = gradle.build()
        println result.standardOutput
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testConfigurations() {
        def result = gradle.withArguments('testConfigurations').build()
        println result.standardOutput
        assert result.task(':testConfigurations').outcome == SUCCESS
    }

    @Test
    void testSourceSets() {
        def result = gradle.withArguments('testSourceSets').build()
        println result.standardOutput
        assert result.task(':testSourceSets').outcome == SUCCESS
    }

    @Test
    void testDependencies() {
        def result = gradle.withArguments('testDependencies').build()
        println result.standardOutput
        assert result.task(':testDependencies').outcome == SUCCESS
    }

    @Test
    void testProcessDataResources() {
        def result = gradle.withArguments('testProcessDataResources').build()
        println result.standardOutput
        // hackery above means this is always up to date, but also always executes:
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':testProcessDataResources').outcome == SUCCESS
        result = gradle.withArguments('testProcessDataResources').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
    }

    @Test(dependsOnMethods = ['testProcessDataResources'])
    void testWav() {
        def result = gradle.withArguments('wav').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == SUCCESS
        result = gradle.withArguments('testWav').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':testWav').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testWav'])
    void testPraatPitchmarker() {
        def result = gradle.withArguments('praatPitchmarker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':praatPitchmarker').outcome == SUCCESS
        result = gradle.withArguments('testPraatPitchmarker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':praatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessDataResources'])
    void testText() {
        def result = gradle.withArguments('text').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':text').outcome == SUCCESS
        result = gradle.withArguments('testText').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':testText').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testText'])
    void testGenerateAllophones() {
        def result = gradle.withArguments('generateAllophones').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == SUCCESS
        result = gradle.withArguments('testGenerateAllophones').build()
        println result.standardOutput
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':testGenerateAllophones').outcome == SUCCESS
    }

    @Test
    void testMaryJavaExec() {
        def result = gradle.withArguments('testMaryJavaExec').build()
        println result.standardOutput
        assert result.task(':testMaryJavaExec').outcome == SUCCESS
    }
}
