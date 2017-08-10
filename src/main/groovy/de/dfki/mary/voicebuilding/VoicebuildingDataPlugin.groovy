package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingDataPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply VoicebuildingBasePlugin

        project.configurations {
            create 'data'
            create 'marytts'
        }

        project.sourceSets {
            create 'data'
        }

        project.dependencies {
            project.afterEvaluate {
                marytts group: 'de.dfki.mary', name: "marytts-lang-$project.voice.locale.language", version: project.maryttsVersion, {
                    exclude module: 'groovy-all'
                }
            }
            marytts group: 'de.dfki.mary', name: "marytts-voicebuilding", version: '0.1'
        }

        project.extensions.create 'voicebuilding', VoicebuildingExtension, project

        project.task('templates', type: CopyClasspathResources) {
            destDir = project.file("$project.buildDir/templates")
        }

        project.task('wav')

        project.voicebuilding.basenames.each { basename ->
            project.task("${basename}_wav", type: SoxExec) {
                dependsOn project.processDataResources
                srcFile = project.file("$project.sourceSets.data.output.resourcesDir/${basename}.wav")
                destFile = project.file("$project.buildDir/wav/${basename}.wav")
                args = ['rate', project.voice.samplingRate]
                project.wav.dependsOn it
            }
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

        project.task('generateHalfPhoneFeatures', type: MaryInterfaceBatchTask) {
            dependsOn project.generateAllophones
            srcDir = project.file("$project.buildDir/prompt_allophones")
            destDir = project.file("$project.buildDir/halfphonefeatures")
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'HALFPHONE_TARGETFEATURES'
            outputExt = 'hpfeats'
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
