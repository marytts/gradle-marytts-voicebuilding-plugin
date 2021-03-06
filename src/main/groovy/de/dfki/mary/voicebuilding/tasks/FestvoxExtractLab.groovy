package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class FestvoxExtractLab extends DefaultTask {

    @InputFiles
    FileCollection srcFiles = project.files()

    @Input
    final MapProperty<String, String> mapping = project.objects.mapProperty(String, String)

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @TaskAction
    void process() {
        project.copy {
            from srcFiles
            into destDir
            include '*.lab'
            filter {
                def label = it.trim().split(/\s+/, 3).last()
                it.trim().replaceAll(label) {
                    mapping.get()[it] ?: it
                }
            }
            fileMode = 0644
            eachFile {
                project.logger.debug "Wrote $it"
            }
        }
    }
}
