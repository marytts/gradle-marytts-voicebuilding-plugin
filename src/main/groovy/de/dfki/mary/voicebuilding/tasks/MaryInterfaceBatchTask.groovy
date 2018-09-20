package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class MaryInterfaceBatchTask extends DefaultTask {

    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @Input
    Property<String> inputType = project.objects.property(String)

    @Input
    Property<String> outputType = project.objects.property(String)

    @Input
    ListProperty<String> outputTypeParams = project.objects.listProperty(String)

    @Input
    Property<String> inputExt = project.objects.property(String)

    @Input
    Property<String> outputExt = project.objects.property(String)

    @Optional
    @Input
    Property<Map> maryttsProperties = project.objects.property(Map)

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @TaskAction
    void process() {
        def batch = []
        project.fileTree(srcDir).include("*.${inputExt.get()}").each { srcFile ->
            def destFile = destDir.file(srcFile.name.replace(inputExt.get(), outputExt.get())).get().asFile
            batch << [
                    locale          : "$project.marytts.voice.maryLocale",
                    inputType       : inputType.get(),
                    inputFile       : "$srcFile",
                    outputType      : outputType.get(),
                    outputTypeParams: outputTypeParams.get(),
                    outputFile      : "$destFile"
            ]
        }
        def batchFile = project.file("$temporaryDir/batch.json")
        batchFile.text = new JsonBuilder(batch).toPrettyString()
        project.javaexec {
            classpath project.configurations.marytts
            main 'marytts.BatchProcessor'
            args batchFile
            systemProperties << maryttsProperties.getOrElse([:])
        }
    }
}
