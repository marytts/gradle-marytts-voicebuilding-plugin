package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class VoicebuildingLegacyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply VoicebuildingDataPlugin

        project.ext {
            legacyBuildDir = project.layout.buildDirectory.dir('mary')
        }

        project.task('processPhoneLabels', type: ProcessPhoneLabels) {
            basenamesFile = project.basenames.destFile
            srcDir = project.layout.buildDirectory.dir('lab')
            destDir = project.layout.buildDirectory.dir('lab_processed')
        }

        project.task('alignLabelsWithPrompts', type: AlignLabelsWithPrompts) {
            basenamesFile = project.basenames.destFile
            labDir = project.layout.buildDirectory.dir('lab')
            maryXmlDir = project.tasks.getByName('generateAllophones').destDir
            destDir = project.layout.buildDirectory.dir('allophones')
        }

        project.task('splitPhoneLabelsIntoHalfPhones', type: SplitPhoneLabelsIntoHalfPhones) {
            basenamesFile = project.basenames.destFile
            srcDir = project.processPhoneLabels.destDir
            destDir = project.layout.buildDirectory.dir('halfphonelab_aligned')
        }

        project.task('phoneUnitFileMaker', type: PhoneUnitFileMaker) {
            basenamesFile = project.basenames.destFile
            srcDir = project.processPhoneLabels.destDir
            srcExt = 'lab'
            pmDir = project.tasks.getByName('pitchmarkConverter').destDir
            sampleRate = project.marytts.voice.samplingRate
            destFile = project.legacyBuildDir.get().file('phoneUnits.mry')
        }

        project.task('halfPhoneUnitFileMaker', type: PhoneUnitFileMaker) {
            basenamesFile = project.basenames.destFile
            srcDir = project.splitPhoneLabelsIntoHalfPhones.destDir
            srcExt = 'hplab'
            pmDir = project.tasks.getByName('pitchmarkConverter').destDir
            sampleRate = project.marytts.voice.samplingRate
            destFile = project.legacyBuildDir.get().file('halfphoneUnits.mry')
        }

        project.task('featureLister', type: FeatureListerTask) {
            destFile = project.legacyBuildDir.get().file('features.txt')
        }

        project.task('phoneUnitFeatureComputer', type: MaryInterfaceBatchTask) {
            dependsOn project.featureLister
            srcDir = project.alignLabelsWithPrompts.destDir
            destDir = project.layout.buildDirectory.dir('phonefeatures')
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'TARGETFEATURES'
            outputExt = 'pfeats'
            doFirst {
                outputTypeParams = ['phone'] + project.featureLister.destFile.get().asFile.readLines().findAll {
                    it != 'phone' && !(it in ['halfphone_lr', 'halfphone_unitname'])
                }
            }
        }

        project.task('generatePhoneFeatureDefinitionFile', type: GeneratePhoneFeatureDefinitionFile) {
            srcDir = project.phoneUnitFeatureComputer.destDir
            srcExt = project.phoneUnitFeatureComputer.outputExt
            destFile = project.legacyBuildDir.get().file('phoneUnitFeatureDefinition.txt')
        }

        project.task('halfPhoneUnitFeatureComputer', type: MaryInterfaceBatchTask) {
            dependsOn project.featureLister
            srcDir = project.alignLabelsWithPrompts.destDir
            destDir = project.layout.buildDirectory.dir('halfphonefeatures')
            inputType = 'ALLOPHONES'
            inputExt = 'xml'
            outputType = 'HALFPHONE_TARGETFEATURES'
            outputExt = 'hpfeats'
            doFirst {
                outputTypeParams = ['halfphone_unitname'] + project.featureLister.destFile.get().asFile.readLines().findAll {
                    it != 'halfphone_unitname'
                }
            }
        }

        project.task('generateHalfPhoneFeatureDefinitionFile', type: GeneratePhoneFeatureDefinitionFile) {
            srcDir = project.halfPhoneUnitFeatureComputer.destDir
            srcExt = project.halfPhoneUnitFeatureComputer.outputExt
            destFile = project.legacyBuildDir.get().file('halfphoneUnitFeatureDefinition.txt')
        }

        project.task('makeBasenameDatagrams', type: MakeBasenameDatagrams) {
            basenamesFile = project.basenames.destFile
            sampleRate = project.marytts.voice.samplingRate
            pmDir = project.tasks.getByName('pitchmarkConverter').destDir
            destDir = project.layout.buildDirectory.dir('basenameDatagrams')
        }

        project.task('basenameTimelineMaker', type: TimelineMaker) {
            basenamesFile = project.basenames.destFile
            sampleRate = project.marytts.voice.samplingRate
            idxIntervalInSeconds = 2.0
            srcDir = project.makeBasenameDatagrams.destDir
            destFile = project.legacyBuildDir.get().file('timeline_basenames.mry')
        }

        project.task('makeWaveDatagrams', type: MakeWaveDatagrams) {
            basenamesFile = project.basenames.destFile
            sampleRate = project.marytts.voice.samplingRate
            wavDir = project.wav.destDir
            pmDir = project.tasks.getByName('pitchmarkConverter').destDir
            destDir = project.layout.buildDirectory.dir('waveDatagrams')
        }

        project.task('waveTimelineMaker', type: TimelineMaker) {
            basenamesFile = project.basenames.destFile
            sampleRate = project.marytts.voice.samplingRate
            idxIntervalInSeconds = 0.1
            srcDir = project.makeWaveDatagrams.destDir
            destFile = project.legacyBuildDir.get().file('timeline_waveforms.mry')
        }

        project.task('makeMcepDatagrams', type: MakeMcepDatagrams) {
            basenamesFile = project.basenames.destFile
            sampleRate = project.marytts.voice.samplingRate
            mcepDir = project.mcepExtractor.destDir
            destDir = project.layout.buildDirectory.dir('mcepDatagrams')
        }

        project.task('generateMcepTimelineHeader', type: GenerateMcepTimelineHeader) {
            srcDir = project.mcepExtractor.destDir
            destFile = project.legacyBuildDir.get().file('timeline_mcep.properties')
        }

        project.task('mcepTimelineMaker', type: McepTimelineMaker) {
            basenamesFile = project.basenames.destFile
            headerFile = project.generateMcepTimelineHeader.destFile
            sampleRate = project.marytts.voice.samplingRate
            idxIntervalInSeconds = 0.1
            srcDir = project.makeMcepDatagrams.destDir
            destFile = project.legacyBuildDir.get().file('timeline_mcep.mry')
        }

        project.task('phoneFeatureFileMaker', type: PhoneFeatureFileMaker) {
            basenamesFile = project.basenames.destFile
            srcDir = project.phoneUnitFeatureComputer.destDir
            srcExt = project.phoneUnitFeatureComputer.outputExt
            unitFile = project.phoneUnitFileMaker.destFile
            featureDefinitionFile = project.generatePhoneFeatureDefinitionFile.destFile
            destFile = project.legacyBuildDir.get().file('phoneFeatures.mry')
        }

        project.task('halfPhoneFeatureFileMaker', type: PhoneFeatureFileMaker) {
            basenamesFile = project.basenames.destFile
            srcDir = project.halfPhoneUnitFeatureComputer.destDir
            srcExt = project.halfPhoneUnitFeatureComputer.outputExt
            unitFile = project.halfPhoneUnitFileMaker.destFile
            featureDefinitionFile = project.generateHalfPhoneFeatureDefinitionFile.destFile
            destFile = project.legacyBuildDir.get().file('halfphoneFeatures.mry')
        }

        project.task('f0ContourFeatureFileMaker', type: F0ContourFeatureFileMaker) {
            featureFile = project.halfPhoneFeatureFileMaker.destFile
            timelineFile = project.waveTimelineMaker.destFile
            unitFile = project.halfPhoneUnitFileMaker.destFile
            gender = project.marytts.voice.gender
            destFile = project.legacyBuildDir.get().file('syllableF0Polynomials.mry')
        }

        project.task('generateAcousticFeatureDefinitionFile', type: GenerateAcousticFeatureDefinitionFile) {
            srcFile = project.halfPhoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('halfphoneUnitFeatureDefinition_ac.txt')
        }

        project.task('acousticFeatureFileMaker', type: AcousticFeatureFileMaker) {
            featureDefinitionFile = project.generateAcousticFeatureDefinitionFile.destFile
            unitFile = project.halfPhoneUnitFileMaker.destFile
            contourFile = project.f0ContourFeatureFileMaker.destFile
            featureFile = project.halfPhoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('halfphoneFeatures_ac.mry')
        }

        project.task('generateJoinCostWeights', type: GenerateJoinCostWeights) {
            destFile = project.legacyBuildDir.get().file('joinCostWeights.txt')
        }

        project.task('joinCostFileMaker', type: JoinCostFileMaker) {
            weightsFile = project.generateJoinCostWeights.destFile
            mcepFile = project.mcepTimelineMaker.destFile
            unitFile = project.halfPhoneUnitFileMaker.destFile
            featureFile = project.acousticFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('joinCostFeatures.mry')
        }

        project.task('generateFeatureSequence', type: GenerateFeatureSequence) {
            features = ['phone']
            destFile = project.legacyBuildDir.get().file('featureSequence.txt')
        }

        project.task('cartBuilder', type: CartBuilder) {
            featureFile = project.acousticFeatureFileMaker.destFile
            featureSequenceFile = project.generateFeatureSequence.destFile
            destFile = project.legacyBuildDir.get().file('cart.mry')
        }

        project.task('generateDurationFeatureDescription', type: GenerateProsodyFeatureDescription) {
            srcFile = project.phoneFeatureFileMaker.destFile
            targetFeatures = ['segment_duration']
            destFile = project.layout.buildDirectory.dir('prosody').get().file('dur.desc')
        }

        project.task('generateF0FeatureDescription', type: GenerateProsodyFeatureDescription) {
            srcFile = project.phoneFeatureFileMaker.destFile
            targetFeatures = ['leftF0', 'midF0', 'rightF0']
            destFile = project.layout.buildDirectory.dir('prosody').get().file('f0.desc')
        }

        project.task('extractDurationFeatures', type: ExtractDurationFeatures) {
            unitFile = project.phoneUnitFileMaker.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            destFile = project.layout.buildDirectory.dir('prosody').get().file('dur.feats')
        }

        project.task('extractF0Features', type: ExtractF0Features) {
            unitFile = project.phoneUnitFileMaker.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            timelineFile = project.waveTimelineMaker.destFile
            destFile = project.layout.buildDirectory.dir('prosody').get().file('f0.feats')
        }

        project.task('trainDurationCart', type: TrainProsodyCart) {
            dataFile = project.extractDurationFeatures.destFile
            descriptionFile = project.generateDurationFeatureDescription.destFile
            predictee = 'segment_duration'
            destFile = project.layout.buildDirectory.dir('prosody').get().file('dur.tree')
        }

        project.task('trainF0LeftCart', type: TrainProsodyCart) {
            dataFile = project.extractF0Features.destFile
            descriptionFile = project.generateF0FeatureDescription.destFile
            predictee = 'leftF0'
            ignoreFields = ['midF0', 'rightF0']
            destFile = project.layout.buildDirectory.dir('prosody').get().file('f0.left.tree')
        }

        project.task('trainF0MidCart', type: TrainProsodyCart) {
            dataFile = project.extractF0Features.destFile
            descriptionFile = project.generateF0FeatureDescription.destFile
            predictee = 'midF0'
            ignoreFields = ['leftF0', 'rightF0']
            destFile = project.layout.buildDirectory.dir('prosody').get().file('f0.mid.tree')
        }

        project.task('trainF0RightCart', type: TrainProsodyCart) {
            dataFile = project.extractF0Features.destFile
            descriptionFile = project.generateF0FeatureDescription.destFile
            predictee = 'rightF0'
            ignoreFields = ['leftF0', 'midF0']
            destFile = project.layout.buildDirectory.dir('prosody').get().file('f0.right.tree')
        }

        project.task('convertDurationCart', type: ConvertProsodyCart) {
            srcFile = project.trainDurationCart.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('dur.tree')
        }

        project.task('convertF0LeftCart', type: ConvertProsodyCart) {
            srcFile = project.trainF0LeftCart.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('f0.left.tree')
        }

        project.task('convertF0MidCart', type: ConvertProsodyCart) {
            srcFile = project.trainF0MidCart.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('f0.mid.tree')
        }

        project.task('convertF0RightCart', type: ConvertProsodyCart) {
            srcFile = project.trainF0RightCart.destFile
            featureFile = project.phoneFeatureFileMaker.destFile
            destFile = project.legacyBuildDir.get().file('f0.right.tree')
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
            from project.generateAcousticFeatureDefinitionFile,
                    project.generateJoinCostWeights,
                    project.cartBuilder,
                    project.convertDurationCart,
                    project.convertF0LeftCart,
                    project.convertF0MidCart,
                    project.convertF0RightCart
            rename { "marytts/voice/$project.marytts.voice.nameCamelCase/$it" }
        }

        project.task('processLegacyResources', type: Copy) {
            from project.waveTimelineMaker,
                    project.basenameTimelineMaker,
                    project.halfPhoneUnitFileMaker,
                    project.acousticFeatureFileMaker,
                    project.joinCostFileMaker
            into project.layout.buildDirectory.dir('legacy')
            rename { "lib/voices/$project.marytts.voice.name/$it" }
        }

        project.run {
            dependsOn project.processLegacyResources
            systemProperty 'mary.base', project.processLegacyResources.destinationDir
        }

        project.integrationTest {
            dependsOn project.processLegacyResources
            systemProperty 'mary.base', project.processLegacyResources.destinationDir
        }

        def legacyZipTask = project.task('legacyZip', type: LegacyZip) {
            from project.processLegacyResources
            from project.jar, {
                rename { "lib/$it" }
            }
            destFile = project.layout.fileProperty()
        }

        def legacyDescriptorTask = project.task('legacyDescriptor', type: LegacyDescriptorTask) {
            project.assemble.dependsOn it
            srcFile = legacyZipTask.destFile
            destFile = project.layout.fileProperty()
        }

        project.artifacts {
            'default' legacyZipTask
        }

        project.publishing {
            publications {
                mavenJava {
                    artifact legacyZipTask
                }
            }
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$project.marytts.voice.language:$project.marytts.version", {
                    exclude group: '*', module: 'groovy-all'
                }
                testCompile "junit:junit:4.12"
            }

            // TODO: legacyZip.archiveName is modified (version is infixed), so we need to update
            def distsDir = project.layout.buildDirectory.dir(project.distsDirName)
            legacyZipTask.destFile.set(distsDir.get().file(legacyZipTask.archiveName))
            legacyDescriptorTask.destFile.set(distsDir.get().file(legacyZipTask.archiveName.replace('.zip', '-component-descriptor.xml')))
        }
    }
}
