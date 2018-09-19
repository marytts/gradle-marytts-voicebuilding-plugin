package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class CopyClasspathResources extends DefaultTask {

    @Input
    ListProperty<String> resources = project.objects.listProperty(String)

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @TaskAction
    void copy() {
        resources.get().each { resourcePath ->
            def stream = this.class.getResourceAsStream(resourcePath)
            if (stream) {
                def resourceName = resourcePath.split('/').last()
                destDir.file(resourceName).get().asFile.withWriter { writer ->
                    writer << stream
                }
            } else {
                project.logger.error "$resourcePath not found on the classpath!"
            }
        }
    }
}
