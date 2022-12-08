package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

class GenerateBasenamesList extends DefaultTask {

    @Optional
    @InputFile
    final RegularFileProperty srcFile = project.objects.fileProperty()

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
            def basenames
            if (srcFile.getOrNull()) {
                basenames = srcFile.get().asFile.readLines().findAll { !it.trim().startsWith('#') }
            } else {
                basenames = project.fileTree(wavDir).matching {
                    include this.includes.getOrElse('*').collect { it + '.wav' }
                    exclude this.excludes.getOrElse([]).collect { it + '.wav' }
                }.collect { it.name - '.wav' }.toSorted()
            }
            basenames.each { basename ->
                def wavFile = wavDir.file("${basename}.wav").get().asFile
                if (!wavFile.canRead())
                    project.logger.warn "WARNING: Could not read from $wavFile"
                def textFile = textDir.file("${basename}.txt").get().asFile
                if (!textFile.canRead())
                    project.logger.warn "WARNING: Could not read from $textFile"
                def labFile = labDir.file("${basename}.lab").get().asFile
                if (!labFile.canRead())
                    project.logger.warn "WARNING: Could not read from $labFile"
                if (wavFile.canRead() && textFile.canRead() && labFile.canRead()) {
                    writer.println basename
                }
            }
        }
    }
}
