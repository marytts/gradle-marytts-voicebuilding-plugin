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

        project.task('bootstrap')

        project.task('templates', type: CopyClasspathResources) {
            destDir = project.file("$project.buildDir/templates")
            resources += ['/de/dfki/mary/voicebuilding/templates/extractPitch.praat',
                          '/de/dfki/mary/voicebuilding/templates/pitchmarks.praat']
            project.bootstrap.dependsOn it
        }

        project.task('wav', type: ProcessWav) {
            dependsOn project.processDataResources
            srcFiles = project.fileTree(project.sourceSets.data.output.resourcesDir).include('*.wav')
            destDir = project.file("$project.buildDir/wav")
        }

        project.task('praatPitchExtractor', type: PraatExtractPitch) {
            dependsOn project.templates, project.wav
            scriptFile = project.file("$project.templates.destDir/extractPitch.praat")
            srcFiles = project.fileTree(project.wav.destDir).include('*.wav')
            destDir = project.file("$project.buildDir/Pitch")
        }

        project.task('praatPitchmarker', type: PraatExtractPitchmarks) {
            dependsOn project.praatPitchExtractor
            scriptFile = project.file("$project.templates.destDir/pitchmarks.praat")
            wavFiles = project.fileTree(project.wav.destDir).include('*.wav')
            pitchFiles = project.fileTree(project.praatPitchExtractor.destDir).include('*.Pitch')
            destDir = project.file("$project.buildDir/PointProcess")
        }

        project.task('pitchmarkConverter', type: PitchmarkConverter) {
            dependsOn project.praatPitchmarker
            srcFiles = project.fileTree(project.praatPitchmarker.destDir).include('*.PointProcess')
            destDir = project.file("$project.buildDir/pm")
        }

        project.task('mcepExtractor', type: ExtractMcep) {
            dependsOn project.wav, project.pitchmarkConverter
            wavFiles = project.fileTree(project.wav.destDir).include('*.wav')
            pmFiles = project.fileTree(project.praatPitchmarker.destDir).include('*.pm')
            destDir = project.file("$project.buildDir/mcep")
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
    }
}
