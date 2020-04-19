package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateFeatureSequence extends DefaultTask {

    @Input
    ListProperty<String> features = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        destFile.get().asFile.withWriter('UTF-8') { dest ->
            dest.println '# Automatically generated feature sequence file for CARTBuilder'
            dest.println '# Add features (one per line) to refine'
            dest.println '# Defines the feature sequence used to build the top-level CART'
            features.get().each { feature ->
                dest.println feature
            }
        }
    }
}
