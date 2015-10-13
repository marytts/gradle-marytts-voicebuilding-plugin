package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import marytts.features.FeatureProcessorManager

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingLegacyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply VoicebuildingDataPlugin

        project.repositories {
            jcenter()
            maven {
                url 'https://oss.jfrog.org/artifactory/repo'
            }
        }

        project.configurations.create 'legacy'

        project.ext {
            maryttsVersion = '5.1.1'
            legacyBuildDir = "$project.buildDir/mary"

            // configure speech-tools
            def proc = 'which ch_track'.execute()
            proc.waitFor()
            speechToolsDir = new File(proc.in.text)?.parentFile?.parent

            // configure praat
            proc = 'which praat'.execute()
            proc.waitFor()
            praat = proc.in.text
        }

        project.task('templates', type: LegacyTemplateTask) {
            destDir = project.file("$project.buildDir/templates")
        }

        project.task('legacyInit', type: LegacyInitTask) {
            dependsOn project.templates
        }

        project.task('legacyPraatPitchmarker', type: LegacyVoiceImportTask) {
            srcDir = project.file("$project.buildDir/wav")
            destDir = project.file("$project.buildDir/pm")
        }

        project.task('legacyMCEPMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPraatPitchmarker
            srcDir = project.file("$project.buildDir/pm")
            destDir = project.file("$project.buildDir/mcep")
        }

        project.task('legacyPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/phonelab")
        }

        project.task('legacyHalfPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/halfphonelab")
        }

        project.task('legacyTranscriptionAligner', type: LegacyVoiceImportTask) {
            dependsOn project.generateAllophones
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/allophones")
        }

        project.task('legacyFeatureLister', type: LegacyFeatureListerTask) {
            destFile = project.file("$project.legacyBuildDir/features.txt")
        }

        project.task('legacyPhoneUnitFeatureGenerator', type: LegacyUnitFeatureGeneratorTask) {
            dependsOn project.legacyTranscriptionAligner, project.legacyFeatureLister
            rootFeature = 'phone'
            exclude = ['halfphone_lr', 'halfphone_unitname']
            outputType = 'TARGETFEATURES'
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/phonefeatures")
            fileExt = 'pfeats'
        }

        project.task('legacyHalfPhoneUnitFeatureGenerator', type: LegacyUnitFeatureGeneratorTask) {
            dependsOn project.legacyTranscriptionAligner, project.legacyFeatureLister
            rootFeature = 'halfphone_unitname'
            exclude = []
            outputType = 'HALFPHONE_TARGETFEATURES'
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/halfphonefeatures")
            fileExt = 'hpfeats'
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$project.voice.language:$project.maryttsVersion"
                legacy("de.dfki.mary:marytts-builder:$project.maryttsVersion") {
                    exclude module: 'mwdumper'
                    exclude module: 'sgt'
                }
                testCompile "junit:junit:4.11"
            }
        }
    }
}
