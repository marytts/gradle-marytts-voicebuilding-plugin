package de.dfki.mary.voicebuilding

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Zip

import groovy.xml.*

import org.apache.commons.codec.digest.DigestUtils

import de.dfki.mary.voicebuilding.tasks.legacy.LegacyVoiceImportTask

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
            outputs.files project.fileTree("$project.buildDir/pm").include('*.pm')
        }

        project.task('legacyMCEPMaker', type: LegacyVoiceImportTask) {
            dependsOn project.legacyInit, project.configureSpeechTools
            inputs.files project.fileTree("$project.buildDir/wav").include('*.wav')
            outputs.files project.fileTree("$project.buildDir/mcep").include('*.mcep')
        }

        project.task('generateAllophones') {
            dependsOn project.legacyInit
            inputs.files project.fileTree("$project.buildDir/text").include('*.txt')
            outputs.files inputs.files.collect {
                new File("$project.buildDir/prompt_allophones", it.name.replace('.txt', '.xml'))
            }
            def mary
            doFirst {
                mary = new marytts.LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.outputType = 'ALLOPHONES'
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = mary.generateXML inFile.text
                    outFile.text = XmlUtil.serialize doc.documentElement
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
                new File("$project.buildDir/lablab", it.name)
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
            outputs.files project.fileTree("$project.buildDir/allophones").include('*.xml')
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
                    fpm = new marytts.features.FeatureProcessorManager(project.voice.maryLocale)
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
                mary = new marytts.LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.inputType = 'ALLOPHONES'
                mary.outputType = 'TARGETFEATURES'
                def features = project.generateFeatureList.featureFile.readLines().minus(['phone', 'halfphone_lr', 'halfphone_unitname']).plus(0, ['phone'])
                mary.outputTypeParams = features.join(' ')
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = marytts.util.dom.DomUtils.parseDocument inFile
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
                mary = new marytts.LocalMaryInterface()
                mary.locale = new Locale(project.voice.maryLocale)
                mary.inputType = 'ALLOPHONES'
                mary.outputType = 'HALFPHONE_TARGETFEATURES'
                def features = project.generateFeatureList.featureFile.readLines().minus(['halfphone_unitname']).plus(0, ['halfphone_unitname'])
                mary.outputTypeParams = features.join(' ')
            }
            doLast {
                [inputs.files as List, outputs.files as List].transpose().each { inFile, outFile ->
                    def doc = marytts.util.dom.DomUtils.parseDocument inFile
                    outFile.text = mary.generateText doc
                }
            }
        }

        project.task('legacyWaveTimelineMaker', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker
            outputs.files new File("$project.legacyBuildDir", 'timeline_waveforms.mry')
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
            outputs.files new File("$project.legacyBuildDir", 'phoneUnits.mry')
        }

        project.task('legacyHalfPhoneUnitfileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPraatPitchmarker, project.legacyHalfPhoneUnitLabelComputer
            outputs.files new File("$project.legacyBuildDir", 'halfphoneUnits.mry')
        }

        project.task('legacyPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.legacyPhoneUnitfileWriter
            outputs.files project.files("$project.legacyBuildDir/phoneFeatures.mry", "$project.legacyBuildDir/phoneUnitFeatureDefinition.txt")
        }

        project.task('legacyHalfPhoneFeatureFileWriter', type: LegacyVoiceImportTask) {
            inputs.files project.generateHalfPhoneUnitFeatures
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
                project.file(serviceLoaderFile.parent).mkdirs()
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
            def destDir = "$project.sourceSets.main.output.resourcesDir/marytts/voice/$voice.nameCamelCase"
            def featureFile = project.file("$destDir/halfphoneUnitFeatureDefinition_ac.txt")
            def joinCostFile = project.file("$destDir/joinCostWeights.txt")
            outputs.files featureFile, joinCostFile
            doFirst {
                project.file(destDir).mkdirs()
            }
            doLast {
                try {
                    project.apply from: 'weights.gradle'
                    def fpm
                    try {
                        fpm = Class.forName("marytts.language.${voice.language}.features.FeatureProcessorManager").newInstance()
                    } catch (e) {
                        logger.info "Reflection failed: $e"
                        logger.info "Instantiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
                        fpm = new marytts.features.FeatureProcessorManager(project.voice.maryLocale)
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
                    logger.info "No weights definition found -- assuming resources are provided..."
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
    }
}
