package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@Deprecated
class LegacyInitTask extends DefaultTask {

    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @OutputFile
    final RegularFileProperty configFile = newOutputFile()

    @TaskAction
    void init() {
        project.copy {
            from srcDir
            into project.buildDir
            include configFile.get().asFile.name
            expand project.properties
        }
    }
}
