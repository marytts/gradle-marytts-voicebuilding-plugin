package de.dfki.mary.voicebuilding.tasks

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.Test

class GenerateBasenamesListFunctionalTest {

    final List DEFAULT_ARGS = ['--warning-mode', 'all', '--stacktrace', '--info']

    @Test
    void 'Given data directories, When basenames list is generated, Then basenames are in sort order'() {
        def projectDir = File.createTempDir()
        def buildDir = new File(projectDir, 'build')

        def basenames = (1..5).collect { i ->
            String.format('test_%04d', i)
        }
        ['wav', 'txt', 'lab'].each { dirName ->
            def dir = new File(projectDir, dirName)
            dir.mkdir()
            basenames.each { basename ->
                new File(dir, "${basename}.${dirName}").createNewFile()
            }
        }

        new File(projectDir, 'build.gradle').text = '''
            plugins {
                id 'de.dfki.mary.voicebuilding-data'
            }
            
            basenames {
                wavDir = file('wav')
                textDir = file('txt')
                labDir = file('lab')
            }
            '''.stripMargin()

        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments(DEFAULT_ARGS + 'basenames')
                .build()

        def expected = basenames
        def actual = new File(buildDir, 'basenames.lst').readLines()
        assert expected == actual
    }

    @Test
    void 'Given data directories, When files are missing, Then basenames excludes them'() {
        def projectDir = File.createTempDir()
        def buildDir = new File(projectDir, 'build')

        def basenames = (1..5).collect { i ->
            String.format('test_%04d', i)
        }
        ['wav', 'txt', 'lab'].each { dirName ->
            def dir = new File(projectDir, dirName)
            dir.mkdir()
            basenames.each { basename ->
                new File(dir, "${basename}.${dirName}").createNewFile()
            }
        }

        new File(projectDir.path + File.separator + 'wav', 'test_0002.wav').delete()
        new File(projectDir.path + File.separator + 'lab', 'test_0004.lab').delete()

        new File(projectDir, 'build.gradle').text = '''
            plugins {
                id 'de.dfki.mary.voicebuilding-data'
            }
            
            basenames {
                wavDir = file('wav')
                textDir = file('txt')
                labDir = file('lab')
            }
            '''.stripMargin()

        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments(DEFAULT_ARGS + 'basenames')
                .build()

        def expected = basenames - ['test_0002', 'test_0004']
        def actual = new File(buildDir, 'basenames.lst').readLines()
        assert expected == actual
    }
}
