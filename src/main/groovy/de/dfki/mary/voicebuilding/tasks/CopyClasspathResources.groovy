package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class CopyClasspathResources extends DefaultTask {

    @OutputDirectory
    File destDir

    @Input
    List resources = []

    @TaskAction
    void copy() {
        resources.each { resourcePath ->
            def stream = this.class.getResourceAsStream(resourcePath)
            if (stream) {
                def resourceName = resourcePath.split('/').last()
                project.file("$destDir/$resourceName").withWriter { writer ->
                    writer << stream
                }
            } else {
                project.logger.error "$resourcePath not found on the classpath!"
            }
        }
    }
}
