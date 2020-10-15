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
        }

        def templateTask = project.task('templates', type: CopyClasspathResources) {
            destDir = project.layout.buildDirectory.dir('templates')
            resources.add '/de/dfki/mary/voicebuilding/templates/extractPitch.praat'
            resources.add '/de/dfki/mary/voicebuilding/templates/pitchmarks.praat'
        }

        def wavTask = project.task('wav', type: ProcessWav) {
            dependsOn project.processDataResources
            srcDir = project.processDataResources.destinationDir
            destDir = project.layout.buildDirectory.dir('wav')
        }

        def basenamesTask = project.task('basenames', type: GenerateBasenamesList) {
            wavDir = wavTask.destDir
            textDir = project.layout.buildDirectory.dir('text')
            labDir = project.layout.buildDirectory.dir('lab')
            destFile = project.layout.buildDirectory.file('basenames.lst')
        }

        def praatPitchExtractorTask = project.task('praatPitchExtractor', type: PraatExtractPitch) {
            dependsOn project.praat, templateTask
            basenamesFile = basenamesTask.destFile
            scriptFile = templateTask.destDir.file('extractPitch.praat')
            srcDir = project.layout.buildDirectory.dir('wav')
            destDir = project.layout.buildDirectory.dir('Pitch')
        }

        def praatPitchmarkerTask = project.task('praatPitchmarker', type: PraatExtractPitchmarks) {
            dependsOn project.praat, templateTask
            basenamesFile = basenamesTask.destFile
            scriptFile = templateTask.destDir.file('pitchmarks.praat')
            wavDir = project.layout.buildDirectory.dir('wav')
            pitchDir = praatPitchExtractorTask.destDir
            destDir = project.layout.buildDirectory.dir('PointProcess')
        }

        def pitchmarkConverterTask = project.task('pitchmarkConverter', type: PitchmarkConverter) {
            basenamesFile = basenamesTask.destFile
            srcDir = praatPitchmarkerTask.destDir
            destDir = project.layout.buildDirectory.dir('pm')
        }

        project.task('mcepExtractor', type: ExtractMcep) {
            basenamesFile = basenamesTask.destFile
            wavDir = project.layout.buildDirectory.dir('wav')
            pmDir = pitchmarkConverterTask.destDir
            destDir = project.layout.buildDirectory.dir('mcep')
        }

        def generateAllophonesTask = project.task('generateAllophones', type: MaryInterfaceBatchTask) {
            srcDir = project.layout.buildDirectory.dir('text')
            destDir = project.layout.buildDirectory.dir('prompt_allophones')
            inputType = 'TEXT'
            inputExt = 'txt'
            outputType = 'ALLOPHONES'
            outputExt = 'xml'
            maryttsProperties = ['mary.base': project.buildDir]
        }

        project.task('generatePhoneFeatures', type: MaryInterfaceBatchTask) {
            srcDir = generateAllophonesTask.destDir
            destDir = project.layout.buildDirectory.dir('phonefeatures')
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'TARGETFEATURES'
            outputExt = 'pfeats'
            maryttsProperties = ['mary.base': project.buildDir]
        }

        project.task('generateHalfPhoneFeatures', type: MaryInterfaceBatchTask) {
            srcDir = generateAllophonesTask.destDir
            destDir = project.layout.buildDirectory.dir('halfphonefeatures')
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'HALFPHONE_TARGETFEATURES'
            outputExt = 'hpfeats'
            maryttsProperties = ['mary.base': project.buildDir]
        }

        project.tasks.withType(MaryInterfaceBatchTask) {
            basenamesFile = basenamesTask.destFile
        }
    }
}
