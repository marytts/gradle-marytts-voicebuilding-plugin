package de.dfki.mary.voicebuilding.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.Test

class GenerateBasenamesListFunctionalTest {

    @Test
    void 'Given data directories, When basenames list is generated, Then basenames are in sort order'() {
        def projectDir = File.createTempDir()

        def basenames = generateBasenames(5)
        createDataDirectories(projectDir, basenames)

        generateBuildScript(projectDir)
        runGradle(projectDir)

        def expected = basenames
        def actual = new File(projectDir, 'basenames.lst').readLines()
        assert expected == actual
    }

    @Test
    void 'Given data directories with some missing files, When basenames list is generated, Then basenames exclude them'() {
        def projectDir = File.createTempDir()

        def basenames = generateBasenames(6)
        createDataDirectories(projectDir, basenames)

        new File(projectDir.path + File.separator + 'wav', 'test_0002.wav').delete()
        new File(projectDir.path + File.separator + 'txt', 'test_0004.txt').delete()
        new File(projectDir.path + File.separator + 'lab', 'test_0006.lab').delete()

        generateBuildScript(projectDir)
        runGradle(projectDir)

        def expected = basenames - ['test_0002', 'test_0004', 'test_0006']
        def actual = new File(projectDir, 'basenames.lst').readLines()
        assert expected == actual
    }

    private static List<String> generateBasenames(int n) {
        (1..n).collect { i ->
            String.format('test_%04d', i)
        }
    }

    private static List<String> createDataDirectories(projectDir, basenames) {
        ['wav', 'txt', 'lab'].each { dirName ->
            def dir = new File(projectDir, dirName)
            dir.mkdir()
            basenames.each { basename ->
                new File(dir, "${basename}.${dirName}").createNewFile()
            }
        }
    }

    private static void generateBuildScript(File projectDir) {
        new File(projectDir, 'build.gradle').text = '''
            plugins {
                id 'de.dfki.mary.voicebuilding-data'
            }
            
            basenames {
                wavDir = file('wav')
                textDir = file('txt')
                labDir = file('lab')
                destFile = file('basenames.lst')
            }
            '''.stripMargin()
    }

    private static BuildResult runGradle(File projectDir) {
        def defaultArgs = ['--warning-mode', 'all', '--stacktrace', '--info']
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments(defaultArgs + 'basenames')
                .build()
    }
}
