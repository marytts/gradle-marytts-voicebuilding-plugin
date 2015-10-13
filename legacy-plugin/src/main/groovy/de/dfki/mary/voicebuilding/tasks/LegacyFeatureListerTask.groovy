package de.dfki.mary.voicebuilding.tasks

import marytts.features.FeatureProcessorManager

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyFeatureListerTask extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        def fpm
        try {
            fpm = Class.forName("marytts.language.${project.voice.language}.features.FeatureProcessorManager").newInstance()
        } catch (e) {
            logger.info "Reflection failed: $e"
            logger.warn "Instantiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
            fpm = new FeatureProcessorManager(project.voice.maryLocale)
        }
        def featureNames = fpm.listByteValuedFeatureProcessorNames() + ' ' + fpm.listShortValuedFeatureProcessorNames()
        destFile.text = featureNames.replace(' ', '\n')
    }
}
