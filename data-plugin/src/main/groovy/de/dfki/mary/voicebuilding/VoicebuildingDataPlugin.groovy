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

        project.task('generateAllophones', type: AllophonesExtractorTask) {
            srcDir = project.file("$project.buildDir/text")
            destDir = project.file("$project.buildDir/prompt_allophones")
        }
    }
}
