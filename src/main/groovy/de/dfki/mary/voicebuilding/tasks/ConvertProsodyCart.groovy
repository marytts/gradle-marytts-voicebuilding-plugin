package de.dfki.mary.voicebuilding.tasks

import marytts.cart.CART
import marytts.cart.LeafNode
import marytts.cart.io.MaryCARTWriter
import marytts.cart.io.WagonCARTReader
import marytts.unitselection.data.FeatureFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ConvertProsodyCart extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void convert() {
        def features = FeatureFileReader.getFeatureFileReader(featureFile.get().asFile.path)
        def featureDefinition = features.featureDefinition
        def root = new WagonCARTReader(LeafNode.LeafType.FloatLeafNode).load(srcFile.get().asFile.newReader('UTF-8'), featureDefinition)
        def cart = new CART(root, featureDefinition)
        new MaryCARTWriter().dumpMaryCART(cart, destFile.get().asFile.path)
    }
}
