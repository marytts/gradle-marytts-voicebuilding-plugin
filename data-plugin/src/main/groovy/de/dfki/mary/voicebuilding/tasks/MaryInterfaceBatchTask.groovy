package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType
import groovy.json.JsonBuilder

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class MaryInterfaceBatchTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @Input
    String inputType

    @Input
    String outputType

    @Optional
    @Input
    List<String> outputTypeParams

    @Input
    String inputExt

    @Input
    String outputExt

    @OutputDirectory
    File destDir

    @TaskAction
    void process() {
        def batch = []
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.$inputExt/) { srcFile ->
            def destFile = new File(destDir, srcFile.name.replace(inputExt, outputExt))
            batch << [
                    locale          : "$project.voice.maryLocale",
                    inputType       : inputType,
                    inputFile       : "$srcFile",
                    outputType      : outputType,
                    outputTypeParams: outputTypeParams,
                    outputFile      : "$destFile"
            ]
        }
        def batchFile = project.file("$temporaryDir/batch.json")
        batchFile.text = new JsonBuilder(batch).toPrettyString()
        project.javaexec {
            classpath project.sourceSets.marytts.runtimeClasspath
            main 'marytts.BatchProcessor'
            args batchFile
        }
    }
}
