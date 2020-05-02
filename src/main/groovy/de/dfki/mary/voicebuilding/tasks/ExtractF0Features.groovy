package de.dfki.mary.voicebuilding.tasks

import marytts.unitselection.data.FeatureFileReader
import marytts.unitselection.data.TimelineReader
import marytts.unitselection.data.UnitFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ExtractF0Features extends DefaultTask {

    @InputFile
    final RegularFileProperty unitFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty timelineFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void extract() {
        def units = new UnitFileReader(unitFile.get().asFile.path)
        def features = FeatureFileReader.getFeatureFileReader(featureFile.get().asFile.path)
        def featureDefinition = features.featureDefinition
        def vowelFeatureName = 'ph_vc'
        def vowelFeatureIndex = featureDefinition.getFeatureIndex(vowelFeatureName)
        def vowelValue = featureDefinition.getFeatureValueAsByte(vowelFeatureIndex, '+')
        def segmentsFromSyllableStartFeatureName = 'segs_from_syl_start'
        def segmentsFromSyllableStartFeatureIndex = featureDefinition.getFeatureIndex(segmentsFromSyllableStartFeatureName)
        def segmentsFromSyllableEndFeatureName = 'segs_from_syl_end'
        def segmentsFromSyllableEndFeatureIndex = featureDefinition.getFeatureIndex(segmentsFromSyllableEndFeatureName)
        def voicedFeatureName = 'ph_cvox'
        def voicedFeatureIndex = featureDefinition.getFeatureIndex(voicedFeatureName)
        def voicedValue = featureDefinition.getFeatureValueAsByte(voicedFeatureIndex, '+')
        def waveTimeline = new TimelineReader(timelineFile.get().asFile.path)
        def sampleRate = waveTimeline.sampleRate
        destFile.get().asFile.withWriter('UTF-8') { dest ->
            for (def unitIndex = 0; unitIndex < units.numberOfUnits; unitIndex++) {
                def unit = units.getUnit(unitIndex)
                def unitFeatures = features.getFeatureVector(unit)
                def unitIsVowel = unitFeatures.getByteFeature(vowelFeatureIndex) == vowelValue
                if (unitIsVowel) {
                    def firstVoicedUnitInSyllable = units.getUnit(unitIndex)
                    def prevUnitIndex = unitIndex - 1
                    def numUnitsBeforeVowelInSyllable = unitFeatures.getByteFeature(segmentsFromSyllableStartFeatureIndex) as int
                    while (prevUnitIndex > unitIndex - numUnitsBeforeVowelInSyllable) {
                        def prevUnitFeatures = features.getFeatureVector(prevUnitIndex)
                        def prevUnitIsNotVoiced = prevUnitFeatures.getByteFeature(voicedFeatureIndex) != voicedValue
                        if (prevUnitIsNotVoiced) {
                            break
                        }
                        firstVoicedUnitInSyllable = units.getUnit(prevUnitIndex)
                        prevUnitIndex--
                    }
                    def lastVoicedUnitInSyllable = units.getUnit(unitIndex)
                    def nextUnitIndex = unitIndex + 1
                    def numUnitsAfterVowelInSyllable = unitFeatures.getByteFeature(segmentsFromSyllableEndFeatureIndex) as int
                    while (nextUnitIndex < unitIndex + numUnitsAfterVowelInSyllable) {
                        def nextUnitFeatures = features.getFeatureVector(nextUnitIndex)
                        def nextUnitIsNotVoiced = nextUnitFeatures.getByteFeature(voicedFeatureIndex) != voicedValue
                        def nextUnitIsNotVowel = nextUnitFeatures.getByteFeature(vowelFeatureIndex) != vowelValue
                        // TODO: why not just check for voiced, which should cover vowels as well?
                        if (nextUnitIsNotVoiced && nextUnitIsNotVowel) {
                            break
                        }
                        lastVoicedUnitInSyllable = units.getUnit(nextUnitIndex)
                        nextUnitIndex++
                    }
                    def midDatagrams = waveTimeline.getDatagrams(unit, sampleRate)
                    def leftDatagrams = waveTimeline.getDatagrams(firstVoicedUnitInSyllable, sampleRate)
                    def rightDatagrams = waveTimeline.getDatagrams(lastVoicedUnitInSyllable, sampleRate)
                    if (midDatagrams && leftDatagrams && rightDatagrams) {
                        def midF0 = sampleRate / midDatagrams[midDatagrams.length / 2 as int].duration as float
                        def leftF0 = sampleRate / leftDatagrams[0].duration as float
                        def rightF0 = sampleRate / rightDatagrams[rightDatagrams.length - 1 as int].duration as float
                        dest.println "$leftF0 $midF0 $rightF0 ${featureDefinition.toFeatureString(unitFeatures)}"
                    }
                    unitIndex = nextUnitIndex - 1
                }
            }
        }
    }
}
