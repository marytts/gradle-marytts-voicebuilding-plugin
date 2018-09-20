package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

class VoicebuildingLegacyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply VoicebuildingDataPlugin

        project.sourceSets.create 'legacy'

        project.configurations.create 'legacy'

        project.ext {
            legacyBuildDir = "$project.buildDir/mary"

            // configure speech-tools
            def proc = 'which ch_track'.execute()
            proc.waitFor()
            speechToolsDir = new File(proc.in.text)?.parentFile?.parent
        }

        project.templates {
            resources.add '/de/dfki/mary/voicebuilding/templates/database.config'
        }

        project.task('legacyInit', type: LegacyInitTask) {
            srcDir = project.tasks.getByName('templates').destDir
            configFile = project.layout.buildDirectory.file('database.config')
        }

        project.task('legacyPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/phonelab_unaligned")
            doLast {
                project.copy {
                    from "$project.buildDir/phonelab"
                    into destDir
                }
            }
        }

        project.task('legacyHalfPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/halfphonelab_unaligned")
            doLast {
                project.copy {
                    from "$project.buildDir/halfphonelab"
                    into destDir
                }
            }
        }

        project.task('legacyTranscriptionAligner', type: LegacyVoiceImportTask) {
            dependsOn project.generateAllophones
            srcDir = project.file("$project.buildDir/lab")
            destDir = project.file("$project.buildDir/allophones")
        }

        project.task('featureLister', type: FeatureListerTask) {
            destFile = project.file("$project.legacyBuildDir/features.txt")
        }

        project.task('phoneUnitFeatureComputer', type: MaryInterfaceBatchTask) {
            dependsOn project.legacyTranscriptionAligner, project.featureLister
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/phonefeatures")
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'TARGETFEATURES'
            outputExt = 'pfeats'
            doFirst {
                outputTypeParams = ['phone'] + project.featureLister.destFile.readLines().findAll {
                    it != 'phone' && !(it in ['halfphone_lr', 'halfphone_unitname'])
                }
            }
        }

        project.task('halfPhoneUnitFeatureComputer', type: MaryInterfaceBatchTask) {
            dependsOn project.legacyTranscriptionAligner, project.featureLister
            srcDir = project.file("$project.buildDir/allophones")
            destDir = project.file("$project.buildDir/halfphonefeatures")
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'HALFPHONE_TARGETFEATURES'
            outputExt = 'hpfeats'
            doFirst {
                outputTypeParams = ['halfphone_unitname'] + project.featureLister.destFile.readLines().findAll {
                    it != 'halfphone_unitname'
                }
            }
        }

        project.task('legacyWaveTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.pitchmarkConverter
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
        }

        project.task('legacyBasenameTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.pitchmarkConverter
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/timeline_basenames.mry")
        }

        project.task('legacyMCepTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.mcepExtractor
            srcDir = project.file("$project.buildDir/wav")
            srcDir2 = project.file("$project.buildDir/mcep")
            destFile = project.file("$project.legacyBuildDir/timeline_mcep.mry")
        }

        project.task('legacyPhoneLabelFeatureAligner', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneUnitLabelComputer, project.phoneUnitFeatureComputer
            srcDir = project.file("$project.buildDir/phonelab_unaligned")
            destDir = project.file("$project.buildDir/phonelab_aligned")
            doLast {
                project.copy {
                    from "$project.buildDir/phonelab"
                    into destDir
                }
            }
        }

        project.task('legacyHalfPhoneLabelFeatureAligner', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitLabelComputer, project.halfPhoneUnitFeatureComputer
            srcDir = project.file("$project.buildDir/halfphonelab_unaligned")
            destDir = project.file("$project.buildDir/halfphonelab_aligned")
            doLast {
                project.copy {
                    from "$project.buildDir/halfphonelab"
                    into destDir
                }
            }
        }

        project.task('legacyPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.pitchmarkConverter, project.legacyPhoneUnitLabelComputer
            dependsOn project.legacyPhoneLabelFeatureAligner
            srcDir = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/phoneUnits.mry")
        }

        project.task('legacyHalfPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.pitchmarkConverter, project.legacyHalfPhoneUnitLabelComputer
            dependsOn project.legacyHalfPhoneLabelFeatureAligner
            srcDir = project.file("$project.buildDir/pm")
            destFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
        }

        project.task('legacyPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneUnitfileWriter, project.phoneUnitFeatureComputer
            srcFile = project.file("$project.legacyBuildDir/phoneUnits.mry")
            srcDir = project.file("$project.buildDir/phonefeatures")
            destFile = project.file("$project.legacyBuildDir/phoneFeatures.mry")
            destFile2 = project.file("$project.legacyBuildDir/phoneUnitFeatureDefinition.txt")
        }

        project.task('legacyHalfPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter
            dependsOn project.halfPhoneUnitFeatureComputer
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcDir = project.file("$project.buildDir/halfphonefeatures")
            destFile = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile2 = project.file("$project.legacyBuildDir/halfphoneUnitFeatureDefinition.txt")
        }

        project.task('legacyF0PolynomialFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter
            dependsOn project.legacyWaveTimelineMaker
            dependsOn project.legacyHalfPhoneFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcFile2 = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
            srcFile3 = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile project.file("$project.legacyBuildDir/syllableF0Polynomials.mry")
        }

        project.task('legacyAcousticFeatureFileWriter', type: LegacyVoiceImportTask) {
            dependsOn project.legacyHalfPhoneUnitfileWriter
            dependsOn project.legacyF0PolynomialFeatureFileWriter
            dependsOn project.legacyHalfPhoneFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcFile2 = project.file("$project.legacyBuildDir/syllableF0Polynomials.mry")
            srcFile3 = project.file("$project.legacyBuildDir/halfphoneFeatures.mry")
            destFile = project.file("$project.legacyBuildDir/halfphoneFeatures_ac.mry")
            destFile2 = project.file("$project.legacyBuildDir/halfphoneUnitFeatureDefinition_ac.txt")
        }

        project.task('legacyJoinCostFileMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyMCepTimelineMaker
            dependsOn project.legacyHalfPhoneUnitfileWriter
            dependsOn project.legacyAcousticFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/timeline_mcep.mry")
            srcFile2 = project.file("$project.legacyBuildDir/halfphoneUnits.mry")
            srcFile3 = project.file("$project.legacyBuildDir/halfphoneFeatures_ac.mry")
            destFile = project.file("$project.legacyBuildDir/joinCostFeatures.mry")
            destFile2 = project.file("$project.legacyBuildDir/joinCostWeights.txt")
        }

        project.task('legacyCARTBuilder', type: LegacyVoiceImportTask) {
            dependsOn project.legacyAcousticFeatureFileWriter
            srcFile = project.file("$project.legacyBuildDir/halfphoneFeatures_ac.mry")
            destFile = project.file("$project.legacyBuildDir/cart.mry")
        }

        project.task('legacyDurationCARTTrainer', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneFeatureFileWriter
            dependsOn project.legacyPhoneUnitfileWriter
            dependsOn project.legacyWaveTimelineMaker
            srcFile = project.file("$project.legacyBuildDir/phoneFeatures.mry")
            srcFile2 = project.file("$project.legacyBuildDir/phoneUnits.mry")
            srcFile3 = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
            destFile = project.file("$project.legacyBuildDir/dur.tree")
        }

        project.task('legacyF0CARTTrainer', type: LegacyVoiceImportTask) {
            dependsOn project.legacyPhoneFeatureFileWriter
            dependsOn project.legacyPhoneUnitfileWriter
            dependsOn project.legacyWaveTimelineMaker
            srcFile = project.file("$project.legacyBuildDir/phoneFeatures.mry")
            srcFile2 = project.file("$project.legacyBuildDir/phoneUnits.mry")
            srcFile3 = project.file("$project.legacyBuildDir/timeline_waveforms.mry")
            destFile = project.file("$project.legacyBuildDir/f0.left.tree")
            destFile2 = project.file("$project.legacyBuildDir/f0.mid.tree")
            destFile3 = project.file("$project.legacyBuildDir/f0.right.tree")
        }

        project.generateVoiceConfig {
            project.afterEvaluate {
                config.get() << [
                        'viterbi.wTargetCosts'    : 0.7,
                        'viterbi.beamsize'        : 100,
                        databaseClass             : 'marytts.unitselection.data.DiphoneUnitDatabase',
                        selectorClass             : 'marytts.unitselection.select.DiphoneUnitSelector',
                        concatenatorClass         : 'marytts.unitselection.concat.OverlapUnitConcatenator',
                        targetCostClass           : 'marytts.unitselection.select.DiphoneFFRTargetCostFunction',
                        joinCostClass             : 'marytts.unitselection.select.JoinCostFeatures',
                        unitReaderClass           : 'marytts.unitselection.data.UnitFileReader',
                        cartReaderClass           : 'marytts.cart.io.MARYCartReader',
                        audioTimelineReaderClass  : 'marytts.unitselection.data.TimelineReader',
                        featureFile               : "MARY_BASE/lib/voices/$project.marytts.voice.name/halfphoneFeatures_ac.mry",
                        targetCostWeights         : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/halfphoneUnitFeatureDefinition_ac.txt",
                        joinCostFile              : "MARY_BASE/lib/voices/$project.marytts.voice.name/joinCostFeatures.mry",
                        joinCostWeights           : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/joinCostWeights.txt",
                        unitsFile                 : "MARY_BASE/lib/voices/$project.marytts.voice.name/halfphoneUnits.mry",
                        cartFile                  : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/cart.mry",
                        audioTimelineFile         : "MARY_BASE/lib/voices/$project.marytts.voice.name/timeline_waveforms.mry",
                        basenameTimeline          : "MARY_BASE/lib/voices/$project.marytts.voice.name/timeline_basenames.mry",
                        acousticModels            : 'duration F0 midF0 rightF0',
                        'duration.model'          : 'cart',
                        'duration.data'           : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/dur.tree",
                        'duration.attribute'      : 'd',
                        'F0.model'                : 'cart',
                        'F0.data'                 : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/f0.left.tree",
                        'F0.attribute'            : 'f0',
                        'F0.attribute.format'     : '(0,%.0f)',
                        'F0.predictFrom'          : 'firstVowels',
                        'F0.applyTo'              : 'firstVoicedSegments',
                        'midF0.model'             : 'cart',
                        'midF0.data'              : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/f0.mid.tree",
                        'midF0.attribute'         : 'f0',
                        'midF0.attribute.format'  : '(50,%.0f)',
                        'midF0.predictFrom'       : 'firstVowels',
                        'midF0.applyTo'           : 'firstVowels',
                        'rightF0.model'           : 'cart',
                        'rightF0.data'            : "jar:/marytts/voice/$project.marytts.voice.nameCamelCase/f0.right.tree",
                        'rightF0.attribute'       : 'f0',
                        'rightF0.attribute.format': '(100,%.0f)',
                        'rightF0.predictFrom'     : 'firstVowels',
                        'rightF0.applyTo'         : 'lastVoicedSegments'
                ]
            }
        }

        project.processResources {
            from project.legacyAcousticFeatureFileWriter, {
                include 'halfphoneUnitFeatureDefinition_ac.txt'
            }
            from project.legacyJoinCostFileMaker, {
                include 'joinCostWeights.txt'
            }
            from project.legacyCARTBuilder
            from project.legacyDurationCARTTrainer
            from project.legacyF0CARTTrainer
            project.afterEvaluate {
                rename { "marytts/voice/$project.marytts.voice.nameCamelCase/$it" }
            }
        }

        project.processLegacyResources {
            from project.legacyWaveTimelineMaker
            from project.legacyBasenameTimelineMaker
            from project.legacyHalfPhoneUnitfileWriter
            from project.legacyAcousticFeatureFileWriter, {
                include 'halfphoneFeatures_ac.mry'
            }
            from project.legacyJoinCostFileMaker, {
                include 'joinCostFeatures.mry'
            }
            project.afterEvaluate {
                rename { "lib/voices/$project.marytts.voice.name/$it" }
            }
        }

        project.run {
            dependsOn project.processLegacyResources
            systemProperty 'mary.base', project.sourceSets.legacy.output.resourcesDir
        }

        project.integrationTest {
            dependsOn project.processLegacyResources
            systemProperty 'mary.base', project.sourceSets.legacy.output.resourcesDir
        }

        project.task('legacyZip', type: Zip) {
            from project.processLegacyResources
            from project.jar, {
                rename { "lib/$it" }
            }
        }

        project.task('legacyDescriptor', type: LegacyDescriptorTask) {
            dependsOn project.legacyZip
            project.assemble.dependsOn it
            project.afterEvaluate {
                srcFile = project.legacyZip.archivePath
                destFile = project.file("$project.distsDir/${project.legacyZip.archiveName.replace('.zip', '-component-descriptor.xml')}")
            }
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$project.marytts.voice.language:$project.marytts.version", {
                    exclude group: '*', module: 'groovy-all'
                }
                legacy("de.dfki.mary:marytts-builder:$project.marytts.version") {
                    exclude group: '*', module: 'mwdumper'
                    exclude group: '*', module: 'sgt'
                }
                testCompile "junit:junit:4.12"
            }
        }
    }
}
