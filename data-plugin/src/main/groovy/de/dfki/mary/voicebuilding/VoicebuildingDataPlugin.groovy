package de.dfki.mary.voicebuilding

import groovy.xml.XmlUtil

import de.dfki.mary.voicebuilding.tasks.*

import marytts.LocalMaryInterface

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class VoicebuildingDataPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin

        project.configurations.create 'data'

        project.sourceSets.create 'data'

        project.task('text', type: TextTask) {
            inputs.files project.processDataResources
            dataFile = project.file("$project.sourceSets.data.output.resourcesDir/$dataFileName")
            textDir = project.file("$project.buildDir/text")
        }

        project.task('generateAllophones', type: AllophonesExtractorTask) {
            inputs.files project.text
            allophonesDir = project.file("$project.buildDir/prompt_allophones")
        }
    }
}
