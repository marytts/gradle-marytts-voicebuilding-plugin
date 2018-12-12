package de.dfki.mary.voicebuilding.tasks

import marytts.unitselection.data.FeatureFileReader
import marytts.unitselection.data.TimelineReader
import marytts.unitselection.data.UnitFileReader
import marytts.unitselection.select.JoinCostFeatures
import marytts.util.data.MaryHeader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class JoinCostFileMaker extends DefaultTask {

    @InputFile
    final RegularFileProperty weightsFile = newInputFile()

    @InputFile
    final RegularFileProperty mcepFile = newInputFile()

    @InputFile
    final RegularFileProperty unitFile = newInputFile()

    @InputFile
    final RegularFileProperty featureFile = newInputFile()

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void make() {
        destFile.get().asFile.withDataOutputStream { dest ->
            def header = new MaryHeader(MaryHeader.JOINFEATS)
            header.writeTo(dest)
            def weightsData = JoinCostFeatures.readJoinCostWeightsFile(weightsFile.get().asFile.path)
            def weights = weightsData[0] as float[]
            def functions = weightsData[1] as String[]
            def numberOfFeatures = weights.length
            dest.writeInt(numberOfFeatures)
            [weights, functions].transpose().each { weight, function ->
                dest.writeFloat(weight)
                dest.writeUTF(function)
            }
            def mceps = new TimelineReader(mcepFile.get().asFile.path)
            def units = new UnitFileReader(unitFile.get().asFile.path)
            def features = new FeatureFileReader(featureFile.get().asFile.path)
            dest.writeInt(units.numberOfUnits)
            def logF0FeatureIndex = features.featureDefinition.getFeatureIndex('unit_logf0')
            def logF0DeltaFeatureIndex = features.featureDefinition.getFeatureIndex('unit_logf0delta')
            (0..units.numberOfUnits - 1).each { u ->
                def unit = units.getUnit(u)
                def unitDuration = unit.duration
                if (unit.isEdgeUnit() || unitDuration == 0) {
                    (numberOfFeatures * 2).times {
                        dest.writeFloat(Float.NaN)
                    }
                } else {
                    // TODO: legacy HalfPhoneLabelFeatureAligner seems to insert zero-duration non-edge units!
                    def unitFeatures = features.getFeatureVector(unit)
                    assert unitDuration > 0
                    def unitStartTime = unit.startTime
                    def leftMcep = mceps.getDatagram(unitStartTime)
                    def logF0 = unitFeatures.getContinuousFeature(logF0FeatureIndex)
                    def logF0Delta = unitFeatures.getContinuousFeature(logF0DeltaFeatureIndex)
                    def leftLogF0 = logF0 - 0.5 * logF0Delta
                    // TODO: this is actually the F0 at the *left* edge of the unit... Is this a bug?
                    def rightLogF0 = logF0 + 0.5 * logF0Delta
                    def rightMcep
                    def unitEndTime = unitStartTime
                    while (unitEndTime < unitStartTime + unitDuration) {
                        rightMcep = mceps.getDatagram(unitEndTime)
                        unitEndTime += rightMcep.duration
                    }
                    dest.write(leftMcep.data)
                    dest.writeFloat(leftLogF0 as float)
                    dest.writeFloat(logF0Delta)
                    dest.write(rightMcep.data)
                    dest.writeFloat(rightLogF0 as float)
                    dest.writeFloat(logF0Delta)
                }
            }
        }
    }
}
