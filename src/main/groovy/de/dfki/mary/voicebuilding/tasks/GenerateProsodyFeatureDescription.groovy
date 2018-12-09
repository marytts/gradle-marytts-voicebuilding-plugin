package de.dfki.mary.voicebuilding.tasks

import marytts.unitselection.data.FeatureFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateProsodyFeatureDescription extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = newInputFile()

    @Input
    ListProperty<String> targetFeatures = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void generate() {
        def features = FeatureFileReader.getFeatureFileReader(srcFile.get().asFile.path)
        def featureDefinition = features.featureDefinition
        destFile.get().asFile.withWriter('UTF-8') { dest ->
            dest.println '('
            targetFeatures.get().each { targetFeature ->
                dest.println "($targetFeature float)"
            }
            def numDiscreteFeatures = featureDefinition.numberOfByteFeatures + featureDefinition.numberOfShortFeatures
            featureDefinition.featureNameArray.eachWithIndex { feature, f ->
                def values = 'float'
                if (f < numDiscreteFeatures) {
                    def possibleFeatureValues = featureDefinition.getPossibleValues(f)
                    if (possibleFeatureValues != (0..19) as String[]) {
                        values = ' ' + possibleFeatureValues.collect {
                            '"' + it.replaceAll('"', '\\\\"') + '"'
                        }.join('  ')
                    }
                }
                dest.println "( $feature $values )"
            }
            dest.println ')'
        }
    }
}
