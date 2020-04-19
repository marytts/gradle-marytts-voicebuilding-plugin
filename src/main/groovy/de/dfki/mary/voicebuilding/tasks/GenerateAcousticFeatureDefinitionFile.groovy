package de.dfki.mary.voicebuilding.tasks

import marytts.unitselection.data.FeatureFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateAcousticFeatureDefinitionFile extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        def features = new FeatureFileReader(srcFile.get().asFile.path)
        def writer = new StringWriter()
        features.featureDefinition.generateFeatureWeightsFile(new PrintWriter(writer))
        destFile.get().asFile.withWriter('UTF-8') { dest ->
            dest << writer.toString()
            dest.println '1000 linear | unit_duration'
            dest.println '100 linear | unit_logf0'
            dest.println '0 linear | unit_logf0delta'
        }
    }
}
