package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

class GenerateBasenamesList extends DefaultTask {

    @InputDirectory
    final DirectoryProperty wavDir = project.objects.directoryProperty()

    @InputDirectory
    final DirectoryProperty textDir = project.objects.directoryProperty()

    @InputDirectory
    final DirectoryProperty labDir = project.objects.directoryProperty()

    @Optional
    @Input
    ListProperty<String> includes = project.objects.listProperty(String)

    @Optional
    @Input
    ListProperty<String> excludes = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    void include(String... includes) {
        this.includes.addAll(includes)
    }

    void exclude(String... excludes) {
        this.excludes.addAll(excludes)
    }

    @TaskAction
    void generate() {
        destFile.get().asFile.withWriter('UTF-8') { writer ->
            project.fileTree(wavDir).matching {
                include this.includes.getOrElse('*').collect { it + '.wav' }
                exclude this.excludes.getOrElse([]).collect { it + '.wav' }
            }.toSorted().each { wavFile ->
                def basename = wavFile.name - '.wav'
                def textFile = textDir.file("${basename}.txt").get().asFile
                if (!textFile.canRead()) {
                    project.logger.warn "WARNING: Could not read from $textFile"
                }
                def labFile = labDir.file("${basename}.lab").get().asFile
                if (!labFile.canRead()) {
                    project.logger.warn "WARNING: Could not read from $labFile"
                }
                if (textFile.canRead() && labFile.canRead()) {
                    writer.println basename
                }
            }
        }
    }
}
