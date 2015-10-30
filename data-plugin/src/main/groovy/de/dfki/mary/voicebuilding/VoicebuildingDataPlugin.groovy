package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingDataPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply VoicebuildingBasePlugin

        project.configurations.create 'data'

        project.sourceSets.create 'data'

        project.task('wav', type: AudioConverterTask) {
            dependsOn project.processDataResources
            srcDir = project.file("$project.sourceSets.data.output.resourcesDir")
            destDir = project.file("$project.buildDir/wav")
        }

        project.task('generateAllophones', type: MaryInterfaceBatchTask) {
            srcDir = project.file("$project.buildDir/text")
            destDir = project.file("$project.buildDir/prompt_allophones")
            inputType = 'TEXT'
            inputExt = 'txt'
            outputType = 'ALLOPHONES'
            outputExt = 'xml'
        }

        project.task('generatePhoneFeatures', type: MaryInterfaceBatchTask) {
            dependsOn project.generateAllophones
            srcDir = project.file("$project.buildDir/prompt_allophones")
            destDir = project.file("$project.buildDir/phonefeatures")
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'TARGETFEATURES'
            outputExt = 'pfeats'
        }

        project.task('praatPitchmarker', type: PraatPitchmarkerTask) {
            dependsOn project.wav
            srcDir = project.file("$project.buildDir/wav")
            destDir = project.file("$project.buildDir/pm")
        }

        project.task('mcepMaker', type: MCEPMakerTask) {
            dependsOn project.praatPitchmarker
            srcDir = project.file("$project.buildDir/wav")
            pmDir = project.file("$project.buildDir/pm")
            destDir = project.file("$project.buildDir/mcep")
        }
    }
}
