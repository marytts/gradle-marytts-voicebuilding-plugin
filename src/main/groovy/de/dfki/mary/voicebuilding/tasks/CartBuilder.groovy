package de.dfki.mary.voicebuilding.tasks

import marytts.cart.FeatureVectorCART
import marytts.cart.impose.FeatureArrayIndexer
import marytts.cart.io.MaryCARTWriter
import marytts.features.FeatureDefinition
import marytts.features.FeatureVector
import marytts.unitselection.data.FeatureFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CartBuilder extends DefaultTask {

    @InputFile
    final RegularFileProperty featureFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureSequenceFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void build() {
        def features = FeatureFileReader.getFeatureFileReader(featureFile.get().asFile.path)
        def featureDefinition = features.featureDefinition
        // remove the feature vectors of edge units
        def edgeIndex = featureDefinition.getFeatureIndex(FeatureDefinition.EDGEFEATURE)
        def featureVectors = features.featureVectors.findAll { !it.isEdgeVector(edgeIndex) }
        // Build the top level tree from a feature sequence
        def featureArrayIndexer = new FeatureArrayIndexer(featureVectors as FeatureVector[], featureDefinition)
        // read feature sequence
        def featureSequence = featureSequenceFile.get().asFile.readLines('UTF-8').findAll { line ->
            !line.trim().startsWith('#')
        }.collect { feature ->
            featureDefinition.getFeatureIndex(feature)
        }
        // sort the features according to feature sequence
        featureArrayIndexer.deepSort(featureSequence as int[])
        // get the resulting tree
        def topLevelTree = featureArrayIndexer.tree
        // convert the top-level CART to Wagon Format
        def topLevelCART = new FeatureVectorCART(topLevelTree, featureArrayIndexer)
        // dump big CART to binary file
        new MaryCARTWriter().dumpMaryCART(topLevelCART, destFile.get().asFile.path)
    }
}
