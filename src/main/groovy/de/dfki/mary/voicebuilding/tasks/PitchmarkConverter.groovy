package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class PitchmarkConverter extends DefaultTask {

    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @TaskAction
    void convert() {
        project.fileTree(srcDir).include('*.PointProcess').each { srcFile ->
            def basename = srcFile.name - '.PointProcess'
            def destFile = destDir.file("${basename}.pm").get().asFile
            def nx
            def times = []
            srcFile.readLines().eachWithIndex { line, l ->
                switch (l) {
                    case { it < 5 }:
                        // ignore praat header...
                        break
                    case 5:
                        // ...up to line 5, which tells number of points
                        nx = line
                        break
                    default:
                        times << line
                }
            }
            destFile.withWriter { pm ->
                pm.println 'EST_File Track'
                pm.println 'DataType ascii'
                pm.println 'NumFrames ' + nx
                pm.println 'NumChannels 0'
                pm.println 'NumAuxChannels 0'
                pm.println 'EqualSpace 0'
                pm.println 'BreaksPresent true'
                pm.println 'CommentChar ;'
                pm.println 'EST_Header_End'
                pm.println times.collect { "${it as float}\t1" }.join('\n')
            }
        }
    }
}
