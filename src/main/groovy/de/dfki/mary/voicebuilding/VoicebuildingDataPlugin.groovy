package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.m2ci.msp.praat.PraatWrapperPlugin

class VoicebuildingDataPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply VoicebuildingBasePlugin
        project.pluginManager.apply PraatWrapperPlugin

        project.configurations {
            create 'data'
            create 'marytts'
        }

        project.sourceSets {
            create 'data'
        }

        project.dependencies {
            project.afterEvaluate {
                marytts group: 'de.dfki.mary', name: "marytts-lang-$project.marytts.voice.locale.language", version: project.marytts.version, {
                    exclude group: '*', module: 'groovy-all'
                }
            }
            marytts group: 'de.dfki.mary', name: "marytts-voicebuilding", version: '0.1'
        }

        project.task('bootstrap')

        project.task('templates', type: CopyClasspathResources) {
            destDir = project.layout.buildDirectory.dir('templates')
            resources.add '/de/dfki/mary/voicebuilding/templates/extractPitch.praat'
            resources.add '/de/dfki/mary/voicebuilding/templates/pitchmarks.praat'
            project.bootstrap.dependsOn it
        }

        def wavTask = project.task('wav', type: ProcessWav) {
            dependsOn project.processDataResources
            srcDir = project.processDataResources.destinationDir
            destDir = project.layout.buildDirectory.dir('wav')
        }

        def praatPitchExtractorTask = project.task('praatPitchExtractor', type: PraatExtractPitch) {
            dependsOn project.praat
            scriptFile = project.templates.destDir.file('extractPitch.praat')
            srcDir = wavTask.destDir
            destDir = project.layout.buildDirectory.dir('Pitch')
        }

        project.task('praatPitchmarker', type: PraatExtractPitchmarks) {
            dependsOn project.praat
            scriptFile = project.templates.destDir.file('pitchmarks.praat')
            wavDir = wavTask.destDir
            pitchDir = praatPitchExtractorTask.destDir
            destDir = project.layout.buildDirectory.dir('PointProcess')
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
