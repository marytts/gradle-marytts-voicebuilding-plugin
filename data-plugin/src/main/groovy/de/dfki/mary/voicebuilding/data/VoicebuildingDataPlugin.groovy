package de.dfki.mary.voicebuilding.data

import groovy.xml.XmlUtil

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

        project.task('generateAllophones') {
            dependsOn project.processDataResources
            inputs.files project.fileTree("$project.sourceSets.data.output.resourcesDir/text").include('*.txt')
            def destDir = project.file("$project.sourceSets.data.output.resourcesDir/prompt_allophones")
            outputs.files inputs.files.collect {
                new File(destDir, it.name.replace('.txt', '.xml'))
            }
            def mary
            def parser = new XmlSlurper(false, false)
            doFirst {
                destDir.mkdirs()
                mary = new LocalMaryInterface()
                // TODO: locale must be configurable
                mary.locale = Locale.US
                mary.outputType = 'ALLOPHONES'
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = mary.generateXML inFile.text
                    def xmlStr = XmlUtil.serialize doc.documentElement
                    def xml = parser.parseText xmlStr
                    outFile.text = XmlUtil.serialize xml
                }
            }
        }
    }
}
