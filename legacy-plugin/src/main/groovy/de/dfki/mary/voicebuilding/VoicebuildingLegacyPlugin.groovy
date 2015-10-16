package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

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

        project.task('legacyPhoneUnitFeatureComputer', type: LegacyUnitFeatureComputerTask) {
            dependsOn project.legacyTranscriptionAligner, project.legacyFeatureLister
            rootFeature = 'phone'
            exclude = ['halfphone_lr', 'halfphone_unitname']
            outputType = 'TARGETFEATURES'
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/phonefeatures")
            fileExt = 'pfeats'
        }

        project.task('legacyHalfPhoneUnitFeatureComputer', type: LegacyUnitFeatureComputerTask) {
            dependsOn project.legacyTranscriptionAligner, project.legacyFeatureLister
            rootFeature = 'halfphone_unitname'
            exclude = []
            outputType = 'HALFPHONE_TARGETFEATURES'
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/halfphonefeatures")
            fileExt = 'hpfeats'
        }

        project.task('legacyWaveTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPraatPitchmarker
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
        }

        project.task('legacyBasenameTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPraatPitchmarker
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/timeline_basenames.mry")
        }

        project.task('legacyMCepTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyMCEPMaker
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/mcep")
            destFile = project.file("$project.legacyBuildDir/timeline_mcep.mry")
        }

        project.task('legacyPhoneLabelFeatureAligner', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneUnitLabelComputer, project.legacyPhoneUnitFeatureComputer
            srcDir = project.file("$project.buildDir/phonelab")
        }

        project.task('legacyHalfPhoneLabelFeatureAligner', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitLabelComputer, project.legacyHalfPhoneUnitFeatureComputer
            srcDir = project.file("$project.buildDir/halfphonelab")
        }

        project.task('legacyPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPraatPitchmarker, project.legacyPhoneUnitLabelComputer
            dependsOn project.legacyPhoneLabelFeatureAligner
            srcDir = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/phoneUnits.mry")
        }

        project.task('legacyHalfPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPraatPitchmarker, project.legacyHalfPhoneUnitLabelComputer
            dependsOn project.legacyHalfPhoneLabelFeatureAligner
            srcDir = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
        }

        project.task('legacyPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneUnitfileWriter, project.legacyPhoneUnitFeatureComputer
            srcFile = project.file("$project.legacyBuildDir/phoneUnits.mry")
            srcDir = project.file("$project.buildDir/phonefeatures")
            destFile = project.file("$project.legacyBuildDir/phoneFeatures.mry")
            destFile2 = project.file("$project.legacyBuildDir/phoneUnitFeatureDefinition.txt")
        }

        project.task('legacyHalfPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter, project.legacyHalfPhoneUnitFeatureComputer
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcDir = project.file("$project.buildDir/halfphonefeatures")
            destFile = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile2 = project.file("$project.legacyBuildDir/halfphoneUnitFeatureDefinition.txt")
        }

        project.task('legacyF0PolynomialFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter, project.legacyWaveTimelineMaker, project.legacyHalfPhoneFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcFile2 = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
            srcFile3 = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile project.file("$project.legacyBuildDir/syllableF0Polynomials.mry")
        }

        project.task('legacyAcousticFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter, project.legacyF0PolynomialFeatureFileWriter, project.legacyHalfPhoneFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcFile2 = project.file("$project.legacyBuildDir/syllableF0Polynomials.mry")
            srcFile3 = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile = project.file("$project.legacyBuildDir/halfphoneFeatures_ac.mry")
            destFile2 = project.file("$project.legacyBuildDir/halfphoneUnitFeatureDefinition_ac.txt")
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
