package de.dfki.mary.voicebuilding

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

import groovy.xml.*

import org.apache.commons.codec.digest.DigestUtils

import de.dfki.mary.voicebuilding.tasks.legacy.LegacyVoiceImportTask

import marytts.LocalMaryInterface
import marytts.cart.CART
import marytts.cart.LeafNode
import marytts.cart.io.MaryCARTWriter
import marytts.cart.io.WagonCARTReader
import marytts.features.FeatureProcessorManager
import marytts.unitselection.data.FeatureFileReader
import marytts.unitselection.data.TimelineReader
import marytts.unitselection.data.UnitFileReader
import marytts.util.dom.DomUtils

class VoicebuildingPlugin implements Plugin<Project> {
    final templateDir = "/de/dfki/mary/voicebuilding/templates"
    def voice
    def license

    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin
        project.plugins.apply IvyPublishPlugin

        project.sourceCompatibility = JavaVersion.VERSION_1_7

        voice = project.extensions.create 'voice', VoicebuildingPluginVoiceExtension, project
        license = project.extensions.create 'license', VoicebuildingPluginLicenseExtension
        project.ext {
            maryttsVersion = '5.1'
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
            legacyBuildDir = "$project.buildDir/mary"
        }

        project.repositories.jcenter()
        project.repositories.maven {
            url 'https://oss.jfrog.org/artifactory/libs-release/'
        }
        project.repositories.mavenLocal()

        project.configurations.create 'legacy'

        project.sourceSets {
            main {
                java {
                    srcDir project.generatedSrcDir
                }
            }
            data
            test {
                java {
                    srcDir project.generatedTestSrcDir
                }
                compileClasspath += data.output
            }
        }

        project.jar.manifest {
            attributes('Created-By': "${System.properties['java.version']} (${System.properties['java.vendor']})",
                    'Built-By': System.properties['user.name'],
                    'Built-With': "gradle-${project.gradle.gradleVersion}, groovy-${GroovySystem.version}")
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$voice.language:$project.maryttsVersion"
                legacy "de.dfki.mary:marytts-builder:$project.maryttsVersion"
                testCompile "junit:junit:4.11"
            }

            addTasks(project)
        }

        project.task('legacyComponentZip', type: Zip) {
            from project.processDataResources
            from(project.jar) {
                rename {
                    "lib/$it"
                }
            }
        }

        project.publishing {
            publications {
                legacyComponent(IvyPublication) {
                    artifact(project.legacyComponentZip) {
                        module "$project.name"
                    }
                }
            }
        }
    }

    private void addTasks(Project project) {

        project.task('configurePraat') {
            def proc = 'which praat'.execute()
            proc.waitFor()
            project.ext.praat = proc.in.text
        }

        project.task('configureSpeechTools') {
            def proc = 'which ch_track'.execute()
            proc.waitFor()
            project.ext.speechToolsDir = new File(proc.in.text)?.parentFile?.parent
        }

        project.task('configureHTK') {
            def proc = 'which HRest'.execute()
            proc.waitFor()
            project.ext.htkDir = new File(proc.in.text)?.parent
        }

        project.task('configureEhmm') {
            def proc = 'which ehmm'.execute()
            proc.waitFor()
            project.ext.ehmmDir = new File(proc.in.text)?.parentFile?.parent
        }

        project.task('legacyInit', type: Copy) {
            description "Initialize DatabaseLayout for legacy VoiceImportTools"
            from project.file(getClass().getResource("$templateDir/database.config"))
            into project.buildDir
            expand project.properties
            doLast {
                project.file(project.legacyBuildDir).mkdirs()
            }
        }

        project.task('legacyPraatPitchmarker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit, project.configurePraat
            inputs.files project.fileTree("$project.buildDir/wav").include('*.wav')
            outputs.files inputs.files.collect {
                new File("$project.buildDir/pm", it.name.replace('.wav', '.pm'))
            }
        }

        project.task('legacyMCEPMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit, project.configureSpeechTools
            inputs.files project.legacyPraatPitchmarker
            outputs.files inputs.files.collect {
                new File("$project.buildDir/mcep", it.name.replace('.pm', '.mcep'))
            }
        }

        project.task('generateAllophones') {
            dependsOn project.legacyInit
            inputs.files project.fileTree("$project.buildDir/text").include('*.txt')
            def destDir = project.file("$project.buildDir/prompt_allophones")
            outputs.files inputs.files.collect {
                new File(destDir, it.name.replace('.txt', '.xml'))
            }
            def mary
            def parser = new XmlSlurper(false, false)
            doFirst {
                destDir.mkdirs()
                mary = new LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.outputType = 'ALLOPHONES'
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = mary.generateXML inFile.text
                    def xmlStr = XmlUtil.serialize doc.documentElement
                    def xml = parser.parseText xmlStr
                    outFile.text = XmlUtil.serialize xml
                }
            }
        }

        project.task('legacyHTKLabeler', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit, project.configureHTK
            inputs.files project.fileTree("$project.buildDir/wav").include('*.wav'), project.generateAllophones
            outputs.files project.fileTree("$project.buildDir/htk/lab").include('*.lab')
        }

        project.task('legacyEHMMLabeler', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit, project.configureEhmm
            inputs.files project.fileTree("$project.buildDir/wav").include('*.wav'), project.generateAllophones
            outputs.files project.fileTree("$project.buildDir/ehmm/lab").include('*.lab')
        }

        project.task('legacyLabelPauseDeleter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyEHMMLabeler
            outputs.files inputs.files.collect {
                new File("$project.buildDir/lab", it.name)
            }
        }

        project.task('legacyPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            inputs.files project.legacyLabelPauseDeleter
            outputs.files inputs.files.collect {
                new File("$project.buildDir/phonelab", it.name)
            }
        }

        project.task('legacyHalfPhoneUnitLabelComputer', type: LegacyVoiceImportTask) {
            inputs.files project.legacyLabelPauseDeleter
            outputs.files inputs.files.collect {
                new File("$project.buildDir/halfphonelab", it.name.replace('.lab', '.hplab'))
            }
        }

        project.task('legacyTranscriptionAligner', type: LegacyVoiceImportTask) {
            inputs.files project.generateAllophones, project.legacyLabelPauseDeleter
            outputs.files project.generateAllophones.outputs.files.collect {
                new File("$project.buildDir/allophones", it.name)
            }
        }

        project.task('generateFeatureList') {
            dependsOn project.legacyInit
            ext.featureFile = project.file("$project.legacyBuildDir/features.txt")
            outputs.files featureFile
            doLast {
                def fpm
                try {
                    fpm = Class.forName("marytts.language.${voice.language}.features.FeatureProcessorManager").newInstance()
                } catch (e) {
                    logger.info "Reflection failed: $e"
                    logger.info "Instantiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
                    fpm = new FeatureProcessorManager(project.voice.maryLocale)
                }
                def featureNames = fpm.listByteValuedFeatureProcessorNames().tokenize() + fpm.listShortValuedFeatureProcessorNames().tokenize()
                featureFile.text = featureNames.join('\n')
            }
        }

        project.task('generatePhoneUnitFeatures') {
            dependsOn project.legacyInit, project.generateFeatureList
            inputs.files project.legacyTranscriptionAligner
            outputs.files inputs.files.collect {
                new File("$project.buildDir/phonefeatures", it.name.replace('.xml', '.pfeats'))
            }
            def mary
            doFirst {
                mary = new LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.inputType = 'ALLOPHONES'
                mary.outputType = 'TARGETFEATURES'
                def features = project.generateFeatureList.featureFile.readLines().minus(['phone', 'halfphone_lr', 'halfphone_unitname']).plus(0, ['phone'])
                mary.outputTypeParams = features.join(' ')
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = DomUtils.parseDocument inFile
                    outFile.text = mary.generateText doc
                }
            }
        }

        project.task('generateHalfPhoneUnitFeatures') {
            dependsOn project.legacyInit, project.generateFeatureList
            inputs.files project.legacyTranscriptionAligner
            outputs.files inputs.files.collect {
                new File("$project.buildDir/halfphonefeatures", it.name.replace('.xml', '.hpfeats'))
            }
            def mary
            doFirst {
                mary = new LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.inputType = 'ALLOPHONES'
                mary.outputType = 'HALFPHONE_TARGETFEATURES'
                def features = project.generateFeatureList.featureFile.readLines().minus(['halfphone_unitname']).plus(0, ['halfphone_unitname'])
                mary.outputTypeParams = features.join(' ')
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = DomUtils.parseDocument inFile
                    outFile.text = mary.generateText doc
                }
            }
        }

        project.task('legacyWaveTimelineMaker', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker
            ext.timelineFile = new File(project.legacyBuildDir, 'timeline_waveforms.mry')
            outputs.files timelineFile
        }

        project.task('legacyBasenameTimelineMaker', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker
            outputs.files new File("$project.legacyBuildDir", 'timeline_basenames.mry')
        }

        project.task('legacyMCepTimelineMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit
            inputs.files project.legacyPraatPitchmarker, project.legacyMCEPMaker
            outputs.files new File("$project.legacyBuildDir", 'timeline_mcep.mry')
        }

        project.task('legacyPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker, project.legacyPhoneUnitLabelComputer
            ext.unitFile = new File(project.legacyBuildDir, 'phoneUnits.mry')
            outputs.files unitFile
        }

        project.task('legacyHalfPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker, project.legacyHalfPhoneUnitLabelComputer
            outputs.files new File("$project.legacyBuildDir", 'halfphoneUnits.mry')
        }

        project.task('legacyPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPhoneUnitfileWriter, project.generatePhoneUnitFeatures
            ext.featureFile = project.file("$project.legacyBuildDir/phoneFeatures.mry")
            outputs.files featureFile, project.file("$project.legacyBuildDir/phoneUnitFeatureDefinition.txt")
        }

        project.task('legacyHalfPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyHalfPhoneUnitfileWriter, project.generateHalfPhoneUnitFeatures
            outputs.files project.files("$project.legacyBuildDir/halfphoneFeatures.mry", "$project.legacyBuildDir/halfphoneUnitFeatureDefinition.txt")
        }

        project.task('legacyF0PolynomialFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyHalfPhoneUnitfileWriter, project.legacyWaveTimelineMaker, project.legacyHalfPhoneFeatureFileWriter
            outputs.files project.file("$project.legacyBuildDir/syllableF0Polynomials.mry")
        }

        project.task('legacyAcousticFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyHalfPhoneUnitfileWriter, project.legacyF0PolynomialFeatureFileWriter, project.legacyHalfPhoneFeatureFileWriter
            outputs.files project.files("$project.legacyBuildDir/halfphoneFeatures_ac.mry", "$project.legacyBuildDir/halfphoneUnitFeatureDefinition_ac.txt")
        }

        project.task('legacyJoinCostFileMaker', type: LegacyVoiceImportTask) {
            inputs.files project.legacyMCEPMaker, project.legacyMCepTimelineMaker, project.legacyHalfPhoneUnitfileWriter, project.legacyAcousticFeatureFileWriter
            outputs.files project.file("$project.legacyBuildDir/joinCostFeatures.mry")
        }

        project.task('legacyCARTBuilder', type: LegacyVoiceImportTask) {
            inputs.files project.legacyAcousticFeatureFileWriter
            outputs.files project.file("$project.legacyBuildDir/cart.mry")
        }

        project.task('extractDurationFeatures') {
            inputs.files project.legacyPhoneFeatureFileWriter, project.legacyPhoneUnitfileWriter
            ext.featsFile = project.file("$temporaryDir/dur.feats")
            outputs.files featsFile
            doLast {
                def featureFile = FeatureFileReader.getFeatureFileReader project.legacyPhoneFeatureFileWriter.featureFile.path
                def featureDefinition = featureFile.featureDefinition
                def unitFile = new UnitFileReader(project.legacyPhoneUnitfileWriter.unitFile.path)
                featsFile.withWriter { feats ->
                    (0..unitFile.numberOfUnits - 1).each { u ->
                        def unit = unitFile.getUnit u
                        def samples = unit.duration
                        def duration = samples / unitFile.sampleRate
                        if (duration > 0.01) {
                            def features = featureFile.getFeatureVector u
                            feats.println "$duration ${featureDefinition.toFeatureString features}"
                        }
                    }
                }
            }
        }

        project.task('generateDurationFeaturesDescription') {
            inputs.files project.legacyPhoneFeatureFileWriter
            ext.descFile = project.file("$temporaryDir/dur.desc")
            outputs.files descFile
            doLast {
                def featureFile = FeatureFileReader.getFeatureFileReader project.legacyPhoneFeatureFileWriter.featureFile.path
                def featureDefinition = featureFile.featureDefinition
                descFile.withWriter { desc ->
                    desc.println '('
                    desc.println '( segment_duration float )'
                    featureDefinition.featureNameArray.eachWithIndex { feature, f ->
                        def values = featureDefinition.getPossibleValues f
                        desc.print "( $feature "
                        if (featureDefinition.isContinuousFeature(f) || values.length == 20 && values.last() == '19') {
                            desc.print 'float'
                        } else {
                            desc.print values.collect { "\"${it.replace '"', '\\\"'}\"" }.join(' ')
                        }
                        desc.println " )"
                    }
                    desc.println " )"
                }
            }
        }

        project.task('trainDurationCart', type: Exec) {
            inputs.files project.extractDurationFeatures, project.generateDurationFeaturesDescription
            def treeFile = project.file("$temporaryDir/dur.tree")
            outputs.files treeFile
            dependsOn project.legacyInit, project.configureSpeechTools
            executable "$project.speechToolsDir/bin/wagon"
            args = [
                    '-data', project.extractDurationFeatures.featsFile,
                    '-desc', project.generateDurationFeaturesDescription.descFile,
                    '-stop', 10,
                    '-output', treeFile
            ]
        }

        project.task('extractF0Features') {
            inputs.files project.legacyPhoneFeatureFileWriter, project.legacyPhoneUnitfileWriter, project.legacyWaveTimelineMaker
            ext.leftFeatsFile = project.file("$temporaryDir/f0.left.feats")
            ext.midFeatsFile = project.file("$temporaryDir/f0.mid.feats")
            ext.rightFeatsFile = project.file("$temporaryDir/f0.right.feats")
            outputs.files leftFeatsFile, midFeatsFile, rightFeatsFile
            doLast {
                // open destination files for writing
                def leftFeats = new FileWriter(leftFeatsFile)
                def midFeats = new FileWriter(midFeatsFile)
                def rightFeats = new FileWriter(rightFeatsFile)
                // MaryTTS files needed to extract F0 baked into unit datagram durations
                def featureFile = FeatureFileReader.getFeatureFileReader project.legacyPhoneFeatureFileWriter.featureFile.path
                def featureDefinition = featureFile.featureDefinition
                def unitFile = new UnitFileReader(project.legacyPhoneUnitfileWriter.unitFile.path)
                def waveTimeline = new TimelineReader(project.legacyWaveTimelineMaker.timelineFile.path)
                // in the absence of high-level feature value accessors, need feature indices
                def numSegsFromSylStartFeatureIndex = featureDefinition.getFeatureIndex 'segs_from_syl_start'
                def numSegsFromEndStartFeatureIndex = featureDefinition.getFeatureIndex 'segs_from_syl_end'
                def phoneFeatureIndex = featureDefinition.getFeatureIndex 'phone'
                def isVowelFeatureIndex = featureDefinition.getFeatureIndex 'ph_vc'
                def isVoicedConsonantFeatureIndex = featureDefinition.getFeatureIndex 'ph_cvox'
                // iterate over all units
                for (def u = 0; u < unitFile.numberOfUnits; u++) {
                    def sylSegs = []
                    // in absence of syllable structure, use segment counter features
                    def featureVector = featureFile.getFeatureVector(u)
                    def firstSegInSyl = u + featureVector.getFeatureAsInt(numSegsFromSylStartFeatureIndex)
                    def lastSegInSyl = u + featureVector.getFeatureAsInt(numSegsFromEndStartFeatureIndex)
                    // reconstruct relevant features per segment in syllable
                    (firstSegInSyl..lastSegInSyl).each {
                        featureVector = featureFile.getFeatureVector it
                        def phone = featureVector.getFeatureAsString(phoneFeatureIndex, featureDefinition)
                        def isVowel = featureVector.getFeatureAsString(isVowelFeatureIndex, featureDefinition) == '+'
                        def isVoicedConsonant = featureVector.getFeatureAsString(isVoicedConsonantFeatureIndex, featureDefinition) == '+'
                        sylSegs << [
                                'unitIndex': it,
                                'phone'    : phone,
                                'isVoiced' : isVowel || isVoicedConsonant,
                                'isVowel'  : isVowel,
                                'features' : featureVector
                        ]
                    }
                    // proceed to reconstruct F0 values from voiced segments, if any
                    def voicedSegs = sylSegs.grep { it['isVowel'] }
                    if (voicedSegs) {
                        // left F0
                        def firstVoicedSeg = voicedSegs.first()
                        def firstVoicedDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(firstVoicedSeg['unitIndex']), unitFile.sampleRate)
                        def leftF0 = waveTimeline.sampleRate / firstVoicedDatagrams.first().duration
                        leftFeats.println "$leftF0 ${featureDefinition.toFeatureString firstVoicedSeg['features']}"
                        // mid F0
                        def firstVowel = voicedSegs.grep { it['isVowel'] }.first()
                        def vowelDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(firstVowel['unitIndex']), unitFile.sampleRate)
                        def midF0 = waveTimeline.sampleRate / vowelDatagrams[vowelDatagrams.length / 2 as int].duration
                        midFeats.println "$midF0 ${featureDefinition.toFeatureString firstVowel['features']}"
                        // right F0
                        def lastVoicedSeg = voicedSegs.last()
                        def lastVoicedDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(lastVoicedSeg['unitIndex']), unitFile.sampleRate)
                        def rightF0 = waveTimeline.sampleRate / lastVoicedDatagrams.last().duration
                        rightFeats.println "$rightF0 ${featureDefinition.toFeatureString lastVoicedSeg['features']}"
                    }
                    // increment to end of syllable
                    u = lastSegInSyl
                }
                leftFeats.close()
                midFeats.close()
                rightFeats.close()
            }
        }

        project.task('generateF0FeaturesDescription') {
            inputs.files project.legacyPhoneFeatureFileWriter
            ext.descFile = project.file("$temporaryDir/f0.desc")
            outputs.files descFile
            doLast {
                def featureFile = FeatureFileReader.getFeatureFileReader project.legacyPhoneFeatureFileWriter.featureFile.path
                def featureDefinition = featureFile.featureDefinition
                descFile.withWriter { desc ->
                    desc.println '('
                    desc.println '( f0 float )'
                    featureDefinition.featureNameArray.eachWithIndex { feature, f ->
                        def values = featureDefinition.getPossibleValues f
                        desc.print "( $feature "
                        if (featureDefinition.isContinuousFeature(f) || values.length == 20 && values.last() == '19') {
                            desc.print 'float'
                        } else {
                            desc.print values.collect { "\"${it.replace '"', '\\\"'}\"" }.join(' ')
                        }
                        desc.println " )"
                    }
                    desc.println " )"
                }
            }
        }

        project.task('trainLeftF0Cart', type: Exec) {
            inputs.files project.extractF0Features.leftFeatsFile, project.generateF0FeaturesDescription
            def treeFile = project.file("$temporaryDir/f0.left.tree")
            outputs.files treeFile
            dependsOn project.legacyInit, project.configureSpeechTools, project.extractF0Features
            executable "$project.speechToolsDir/bin/wagon"
            args = [
                    '-data', project.extractF0Features.leftFeatsFile,
                    '-desc', project.generateF0FeaturesDescription.descFile,
                    '-stop', 10,
                    '-output', treeFile
            ]
        }

        project.task('trainMidF0Cart', type: Exec) {
            inputs.files project.extractF0Features.midFeatsFile, project.generateF0FeaturesDescription
            def treeFile = project.file("$temporaryDir/f0.mid.tree")
            outputs.files treeFile
            dependsOn project.legacyInit, project.configureSpeechTools, project.extractF0Features
            executable "$project.speechToolsDir/bin/wagon"
            args = [
                    '-data', project.extractF0Features.midFeatsFile,
                    '-desc', project.generateF0FeaturesDescription.descFile,
                    '-stop', 10,
                    '-output', treeFile
            ]
        }

        project.task('trainRightF0Cart', type: Exec) {
            inputs.files project.extractF0Features.rightFeatsFile, project.generateF0FeaturesDescription
            def treeFile = project.file("$temporaryDir/f0.right.tree")
            outputs.files treeFile
            dependsOn project.legacyInit, project.configureSpeechTools, project.extractF0Features
            executable "$project.speechToolsDir/bin/wagon"
            args = [
                    '-data', project.extractF0Features.rightFeatsFile,
                    '-desc', project.generateF0FeaturesDescription.descFile,
                    '-stop', 10,
                    '-output', treeFile
            ]
        }

        project.task('processCarts') {
            inputs.files project.trainDurationCart, project.trainLeftF0Cart, project.trainMidF0Cart, project.trainRightF0Cart
            outputs.files inputs.files.collect {
                new File(project.legacyBuildDir, it.name)
            }
            dependsOn project.legacyPhoneFeatureFileWriter
            doLast {
                def featureFile = FeatureFileReader.getFeatureFileReader project.legacyPhoneFeatureFileWriter.featureFile.path
                def featureDefinition = featureFile.featureDefinition
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def wagonCartReader = new WagonCARTReader(LeafNode.LeafType.FloatLeafNode)
                    def rootNode = wagonCartReader.load(new BufferedReader(new FileReader(inFile)), featureDefinition)
                    def cart = new CART(rootNode, featureDefinition)
                    def maryCartWriter = new MaryCARTWriter()
                    maryCartWriter.dumpMaryCART(cart, outFile.path);
                }
            }
        }

        project.task('generateSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/Config.java"))
            into project.generatedSrcDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            expand project.properties
        }

        project.compileJava.dependsOn project.generateSource

        project.task('generateTestSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/ConfigTest.java"))
            from project.file(getClass().getResource("$templateDir/LoadVoiceIT.java"))
            into project.generatedTestSrcDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            expand project.properties
        }

        project.compileTestJava.dependsOn project.generateTestSource

        project.task('generateServiceLoader') {
            def serviceLoaderFile = project.file("$project.sourceSets.main.output.resourcesDir/META-INF/services/marytts.config.MaryConfig")
            outputs.files serviceLoaderFile
            doFirst {
                serviceLoaderFile.parentFile.mkdirs()
            }
            doLast {
                serviceLoaderFile.text = "marytts.voice.${voice.nameCamelCase}.Config"
            }
        }

        project.task('generateVoiceConfig', type: Copy) {
            from project.file(getClass().getResource("$templateDir/voice${project.voice.type == "hsmm" ? "-hsmm" : ""}.config"))
            into project.sourceSets.main.output.resourcesDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/voice.config"
            }
            expand project.properties
        }

        project.task('generateFeatureFiles') {
            def destDir = project.file("$project.sourceSets.main.output.resourcesDir/marytts/voice/$voice.nameCamelCase")
            def featureFile = new File(destDir, 'halfphoneUnitFeatureDefinition_ac.txt')
            def joinCostFile = new File(destDir, 'joinCostWeights.txt')
            outputs.files featureFile, joinCostFile
            doLast {
                try {
                    project.apply from: 'weights.gradle'
                    def fpm
                    try {
                        fpm = Class.forName("marytts.language.${voice.language}.features.FeatureProcessorManager").newInstance()
                    } catch (e) {
                        logger.info "Reflection failed: $e"
                        logger.info "Instantiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
                        fpm = new FeatureProcessorManager(project.voice.maryLocale)
                    }
                    featureFile.withWriter { dest ->
                        dest.println 'ByteValuedFeatureProcessors'
                        fpm.listByteValuedFeatureProcessorNames().tokenize().sort { a, b ->
                            if (a == 'halfphone_unitname') return -1
                            if (b == 'halfphone_unitname') return 1
                            a <=> b
                        }.each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).values.join(' ')
                            dest.println "$weight | $name $values"
                        }
                        dest.println 'ShortValuedFeatureProcessors'
                        fpm.listShortValuedFeatureProcessorNames().tokenize().each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).values.join(' ')
                            dest.println "$weight | $name $values"
                        }
                        dest.println 'ContinuousFeatureProcessors'
                        fpm.listContinuousFeatureProcessorNames().tokenize().each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            dest.println "$weight | $name"
                        }
                    }
                    joinCostFile.withWriter { dest ->
                        (0..13).each { name ->
                            def weight = project.featureWeights[name] ?: '1.0 linear'
                            dest.println "${name.toString().padRight(2)} : $weight"
                        }
                    }
                } catch (e) {
                    logger.warn "No weights definition found -- assuming resources are provided..."
                }
            }
        }

        project.processResources {
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            dependsOn project.generateServiceLoader, project.generateVoiceConfig
            if (project.voice.type == 'unit selection') {
                dependsOn project.generateFeatureFiles
            }
        }

        project.processDataResources.rename {
            "lib/voices/$voice.name/$it"
        }

        project.test {
            systemProperty 'mary.base', project.sourceSets.data.output.resourcesDir
            systemProperty 'log4j.logger.marytts', 'DEBUG,stderr'
            maxHeapSize = '1g'
        }

        project.task('generatePom') {
            def pomDir = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace '.', '/'}/$project.name")
            def pomFile = project.file("$pomDir/pom.xml")
            def propFile = project.file("$pomDir/pom.properties")
            outputs.files project.files(pomFile, propFile)
            doFirst {
                pomDir.mkdirs()
            }
            doLast {
                project.pom { pom ->
                    pom.project {
                        description voice.description
                        licenses {
                            license {
                                name project.license.name
                                url project.license.url
                            }
                        }
                    }
                }.writeTo(pomFile)
                propFile.withWriter { dest ->
                    dest.println "version=$project.version"
                    dest.println "groupId=$project.group"
                    dest.println "artifactId=$project.name"
                }
            }
        }

        project.jar.dependsOn project.generatePom

        project.task('legacyComponentXml') {
            dependsOn project.legacyComponentZip
            def zipFile = project.legacyComponentZip.outputs.files.singleFile
            def xmlFile = project.file("$project.distsDir/$project.name-$project.version-component-descriptor.xml")
            inputs.files zipFile
            outputs.files xmlFile
            doLast {
                def zipFileHash = DigestUtils.md5Hex(new FileInputStream(zipFile))
                def builder = new StreamingMarkupBuilder()
                def xml = builder.bind {
                    'marytts-install'(xmlns: 'http://mary.dfki.de/installer') {
                        voice(gender: voice.gender, locale: voice.maryLocale, name: voice.name, type: voice.type, version: project.version) {
                            delegate.description voice.description
                            license(href: license.url)
                            'package'(filename: zipFile.name, md5sum: zipFileHash, size: zipFile.size()) {
                                location(folder: true, href: "http://mary.dfki.de/download/$project.maryttsVersion/")
                            }
                            depends(language: voice.maryLocaleXml, version: project.maryttsVersion)
                        }
                    }
                }
                xmlFile.text = XmlUtil.serialize(xml)
            }
        }

        project.task('visualizeTaskDependencyGraph') << {
            project.file('build.dot').withWriter { dot ->
                dot.println 'digraph G {'
                dot.println 'rankdir=BT;'
                dot.println 'node [shape=box];'
                dot.println 'edge [dir="back"];'
                project.tasks.each { task ->
                    dot.println "$task.name;"
                    task.taskDependencies.getDependencies(task).each { otherTask ->
                        dot.println "$task.name -> $otherTask.name;"
                    }
                }
                dot.println "}"
            }
        }
    }
}
