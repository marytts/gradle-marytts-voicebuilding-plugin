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
            resources = ['/de/dfki/mary/voicebuilding/templates/extractPitch.praat',
                         '/de/dfki/mary/voicebuilding/templates/pitchmarks.praat']
        }

        project.task('wav')

        project.task('praatPitchExtractor')

        project.task('praatPitchmarker')

        project.task('pitchmarkConverter')

        project.voicebuilding.basenames.each { basename ->
            project.task("${basename}_wav", type: SoxExec) {
                dependsOn project.processDataResources
                srcFile = project.file("$project.sourceSets.data.output.resourcesDir/${basename}.wav")
                destFile = project.file("$project.buildDir/wav/${basename}.wav")
                args = ['rate', project.voice.samplingRate]
                project.wav.dependsOn it
            }

            project.task("${basename}_extractPitch", type: PraatExec) {
                def wavTask = project.tasks.getByName("${basename}_wav")
                dependsOn project.templates, wavTask
                srcFiles << wavTask.destFile
                destFile = project.file("$project.buildDir/pm/${basename}.Pitch")
                scriptFile = project.file("$project.templates.destDir/extractPitch.praat")
                project.praatPitchExtractor.dependsOn it
                project.afterEvaluate {
                    props = [wavFile  : wavTask.destFile,
                             pitchFile: destFile,
                             minPitch : (project.voice.gender == 'female') ? 100 : 75,
                             maxPitch : (project.voice.gender == 'female') ? 500 : 300]
                }
            }

            project.task("${basename}_extractPitchmarks", type: PraatExec) {
                def wavTask = project.tasks.getByName("${basename}_wav")
                def pitchTask = project.tasks.getByName("${basename}_extractPitch")
                dependsOn project.templates, wavTask, pitchTask
                srcFiles << [wavTask.destFile, pitchTask.destFile]
                destFile = project.file("$project.buildDir/pm/${basename}.PointProcess")
                scriptFile = project.file("$project.templates.destDir/pitchmarks.praat")
                props = [wavFile   : wavTask.destFile,
                         pitchFile : pitchTask.destFile,
                         pointpFile: destFile]
                project.praatPitchmarker.dependsOn it
            }

            project.task("${basename}_convertPitchmarks", type: PitchmarkConverter) {
                def pitchmarkTask = project.tasks.getByName("${basename}_extractPitchmarks")
                dependsOn pitchmarkTask
                srcFile = pitchmarkTask.destFile
                destFile = project.file("$project.buildDir/pm/${basename}.pm")
                project.pitchmarkConverter.dependsOn it
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

        project.task('mcepMaker', type: MCEPMakerTask) {
            dependsOn project.praatPitchmarker
            srcDir = project.file("$project.buildDir/wav")
            pmDir = project.file("$project.buildDir/pm")
            destDir = project.file("$project.buildDir/mcep")
        }
    }
}
