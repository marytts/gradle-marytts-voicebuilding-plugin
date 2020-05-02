package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class PhoneUnitFeatureLister extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = project.objects.fileProperty()

    @Input
    final Property<String> featureToListFirst = project.objects.property(String)

    @Optional
    @Input
    final ListProperty<String> featuresToExclude = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void list() {
        def features = srcFile.get().asFile.readLines() - featureToListFirst.get()
        features.removeAll(featuresToExclude.get())
        features.push(featureToListFirst.get())
        destFile.get().asFile.withWriter { dest ->
            features.each { feature ->
                dest.println feature
            }
        }
    }
}
