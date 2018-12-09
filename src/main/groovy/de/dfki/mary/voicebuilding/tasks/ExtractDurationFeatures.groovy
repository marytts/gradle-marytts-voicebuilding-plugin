package de.dfki.mary.voicebuilding.tasks

import marytts.unitselection.data.FeatureFileReader
import marytts.unitselection.data.UnitFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ExtractDurationFeatures extends DefaultTask {

    @InputFile
    final RegularFileProperty unitFile = newInputFile()

    @InputFile
    final RegularFileProperty featureFile = newInputFile()

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void extract() {
        def units = new UnitFileReader(unitFile.get().asFile.path)
        def features = FeatureFileReader.getFeatureFileReader(featureFile.get().asFile.path)
        def featureDefinition = features.featureDefinition
        destFile.get().asFile.withWriter('UTF-8') { dest ->
            (0..units.numberOfUnits - 1).each { u ->
                def unit = units.getUnit(u)
                def duration = unit.duration / units.sampleRate as float
                if (duration >= 0.01) {
                    def featureValues = featureDefinition.toFeatureString(features.getFeatureVector(unit))
                    dest.println "$duration $featureValues"
                }
            }
        }
    }
}
