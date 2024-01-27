package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class MaryInterfaceBatchTask extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @Input
    final Property<String> inputType = project.objects.property(String)

    @Input
    final Property<String> outputType = project.objects.property(String)

    @Input
    final ListProperty<String> outputTypeParams = project.objects.listProperty(String)

    @Optional
    @InputFile
    final RegularFileProperty outputTypeParamsFile = project.objects.fileProperty()

    @Input
    final Property<String> inputExt = project.objects.property(String)

    @Input
    final Property<String> outputExt = project.objects.property(String)

    @Optional
    @Input
    final MapProperty<String, Object> maryttsProperties = project.objects.mapProperty(String, Object)

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @TaskAction
    void process() {
        def batch = []
        def outputTypeParams = this.outputTypeParams.get()
        if (outputTypeParamsFile.getOrNull()) {
            outputTypeParams = outputTypeParamsFile.get().asFile.readLines()
        }
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def srcFile = srcDir.file("${basename}.${inputExt.get()}").get().asFile
            def destFile = destDir.file(srcFile.name.replace(inputExt.get(), outputExt.get())).get().asFile
            batch << [
                    locale          : "$project.marytts.voice.maryLocale",
                    inputType       : inputType.get(),
                    inputFile       : "$srcFile",
                    outputType      : outputType.get(),
                    outputTypeParams: outputTypeParams,
                    outputFile      : "$destFile"
            ]
        }
        def batchFile = project.file("$temporaryDir/batch.json")
        batchFile.text = new JsonBuilder(batch).toPrettyString()
        project.javaexec {

            classpath project.configurations.marytts
            classpath this.class.classLoader.URLs

            main 'de.dfki.mary.voicebuilding.utils.BatchProcessor'
            args batchFile
            systemProperties << maryttsProperties.getOrElse([:])
        }
    }
}
