package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def maryVersion = System.properties.maryVersion
    def group = 'de.dfki.mary'
    def version = '1.2.3'
    def voiceName = 'cmu-time-awb'
    def voiceNameCamelCase = 'CmuTimeAwb'
    def voiceGender = 'male'
    def voiceLocale = Locale.UK
    def voiceLicenseUrl = 'http://mary.dfki.de/download/arctic-license.html'

    @BeforeSuite
    void setup() {
        def testKitDir = new File(System.properties.testKitDir)

        def projectDir = new File(System.properties.testProjectDir)
        projectDir.mkdirs()
        buildFile = new File(projectDir, 'build.gradle')

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { new File(it) }

        gradle = GradleRunner.create().withTestKitDir(testKitDir).withProjectDir(projectDir).withPluginClasspath(pluginClasspath)

        // Add the logic under test to the test build
        buildFile.text = """
        buildscript {
            repositories {
                jcenter()
            }
            dependencies {
                classpath group: 'xmlunit', name: 'xmlunit', version: '1.6'
            }
        }

        plugins {
            id 'de.dfki.mary.voicebuilding-legacy'
            id 'de.dfki.mary.voicebuilding-festvox'
        }

        group "$group"
        version "$version"

        voice {
            name = "$voiceName"
            gender = "$voiceGender"
            region = "$voiceLocale.country"
            license {
                url = "$voiceLicenseUrl"
            }
        }

        repositories {
            ivy {
                url 'https://dl.bintray.com/marytts/marytts'
                layout 'pattern', {
                    artifact '[organisation]/[module]/[artifact].[ext]'
                }
            }
            ivy {
                url 'http://festvox.org/examples'
                layout 'pattern', {
                    artifact '[module]_[classifier]/packed/[artifact].[ext]'
                }
            }
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', classifier: 'ldom', ext: 'tar.bz2'
        }

        text.srcFileName = 'time.data'

        legacyInit.dependsOn wav, text, lab

        generateAllophones.dependsOn legacyInit

        task testPlugins(group: 'Verification') << {
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-legacy')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-base')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-data')
            assert plugins.findPlugin('de.dfki.mary.voicebuilding-festvox')
        }

        task testTemplates(group: 'Verification') {
            dependsOn templates
            doLast {
                assert fileTree(buildDir).include('templates/*.config').files
            }
        }

        task testLegacyInit(group: 'Verification') {
            dependsOn legacyInit
            doLast {
                assert file("\$buildDir/database.config").exists()
            }
        }

        task testLegacyPraatPitchmarker(group: 'Verification') {
            dependsOn legacyPraatPitchmarker
            doLast {
                assert fileTree(buildDir).include('pm/*.PointProcess').files
                assert fileTree(buildDir).include('pm/*.pm').files
            }
        }

        task testLegacyMCEPMaker(group: 'Verification') {
            dependsOn legacyMCEPMaker
            doLast {
                assert fileTree(buildDir).include('mcep/*.mcep').files
            }
        }

        task testLegacyPhoneUnitLabelComputer(group: 'Verification') {
            dependsOn legacyPhoneUnitLabelComputer
            doLast {
                assert fileTree(buildDir).include('phonelab/*.lab').files
            }
        }

        task testLegacyHalfPhoneUnitLabelComputer(group: 'Verification') {
            dependsOn legacyHalfPhoneUnitLabelComputer
            doLast {
                assert fileTree(buildDir).include('halfphonelab/*.hplab').files
            }
        }

        task testLegacyTranscriptionAligner(group: 'Verification') {
            dependsOn legacyTranscriptionAligner
            doLast {
                assert fileTree(buildDir).include('allophones/*.xml').files
            }
        }

        task testFeatureLister(group: 'Verification') {
            dependsOn featureLister
            doLast {
                assert file("\$buildDir/mary/features.txt").exists()
            }
        }

        task testPhoneUnitFeatureComputer(group: 'Verification') {
            dependsOn phoneUnitFeatureComputer
            doLast {
                assert fileTree(buildDir).include('phonefeatures/*.pfeats').files
            }
        }

        task testHalfPhoneUnitFeatureComputer(group: 'Verification') {
            dependsOn halfPhoneUnitFeatureComputer
            doLast {
                assert fileTree(buildDir).include('halfphonefeatures/*.hpfeats').files
            }
        }

        task testLegacyWaveTimelineMaker(group: 'Verification') {
            dependsOn legacyWaveTimelineMaker
            doLast {
                assert file("\$buildDir/mary/timeline_waveforms.mry").exists()
            }
        }

        task testLegacyBasenameTimelineMaker(group: 'Verification') {
            dependsOn legacyBasenameTimelineMaker
            doLast {
                assert file("\$buildDir/mary/timeline_basenames.mry").exists()
            }
        }

        task testLegacyMCepTimelineMaker(group: 'Verification') {
            dependsOn legacyMCepTimelineMaker
            doLast {
                assert file("\$buildDir/mary/timeline_mcep.mry").exists()
            }
        }

        task testLegacyPhoneUnitfileWriter(group: 'Verification') {
            dependsOn legacyPhoneUnitfileWriter
            doLast {
                assert file("\$buildDir/mary/phoneUnits.mry").exists()
            }
        }

        task testLegacyHalfPhoneUnitfileWriter(group: 'Verification') {
            dependsOn legacyHalfPhoneUnitfileWriter
            doLast {
                assert file("\$buildDir/mary/halfphoneUnits.mry").exists()
            }
        }

        task testLegacyPhoneFeatureFileWriter(group: 'Verification') {
            dependsOn legacyPhoneFeatureFileWriter
            doLast {
                assert file("\$buildDir/mary/phoneFeatures.mry").exists()
                assert file("\$buildDir/mary/phoneUnitFeatureDefinition.txt").exists()
            }
        }

        task testLegacyHalfPhoneFeatureFileWriter(group: 'Verification') {
            dependsOn legacyHalfPhoneFeatureFileWriter
            doLast {
                assert file("\$buildDir/mary/halfphoneFeatures.mry").exists()
                assert file("\$buildDir/mary/halfphoneUnitFeatureDefinition.txt").exists()
            }
        }

        task testLegacyF0PolynomialFeatureFileWriter(group: 'Verification') {
            dependsOn legacyF0PolynomialFeatureFileWriter
            doLast {
                assert file("\$buildDir/mary/syllableF0Polynomials.mry").exists()
            }
        }

        task testLegacyAcousticFeatureFileWriter(group: 'Verification') {
            dependsOn legacyAcousticFeatureFileWriter
            doLast {
                assert file("\$buildDir/mary/halfphoneFeatures_ac.mry").exists()
                assert file("\$buildDir/mary/halfphoneUnitFeatureDefinition_ac.txt").exists()
            }
        }

        task testLegacyJoinCostFileMaker(group: 'Verification') {
            dependsOn legacyJoinCostFileMaker
            doLast {
                assert file("\$buildDir/mary/joinCostFeatures.mry").exists()
                assert file("\$buildDir/mary/joinCostWeights.txt").exists()
            }
        }

        task testLegacyCARTBuilder(group: 'Verification') {
            dependsOn legacyCARTBuilder
            doLast {
                assert file("\$buildDir/mary/cart.mry").exists()
            }
        }

        task testLegacyDurationCARTTrainer(group: 'Verification') {
            dependsOn legacyDurationCARTTrainer
            doLast {
                assert file("\$buildDir/mary/dur.tree").exists()
            }
        }

        task testLegacyF0CARTTrainer(group: 'Verification') {
            dependsOn legacyF0CARTTrainer
            doLast {
                assert file("\$buildDir/mary/f0.left.tree").exists()
                assert file("\$buildDir/mary/f0.mid.tree").exists()
                assert file("\$buildDir/mary/f0.right.tree").exists()
            }
        }

        task testProcessResources(group: 'Verification') {
            dependsOn processResources
            doLast {
                def prefix = "\$sourceSets.main.output.resourcesDir/marytts/voice/$voiceNameCamelCase"
                assert file("\$prefix/cart.mry").exists()
                assert file("\$prefix/dur.tree").exists()
                assert file("\$prefix/f0.left.tree").exists()
                assert file("\$prefix/f0.mid.tree").exists()
                assert file("\$prefix/f0.right.tree").exists()
                assert file("\$prefix/halfphoneUnitFeatureDefinition_ac.txt").exists()
                assert file("\$prefix/joinCostWeights.txt").exists()
            }
        }

        task testProcessLegacyResources(group: 'Verification') {
            dependsOn processLegacyResources
            doLast {
                def prefix = "\$sourceSets.legacy.output.resourcesDir/lib/voices/$voiceName"
                assert file("\$prefix/halfphoneFeatures_ac.mry").exists()
                assert file("\$prefix/halfphoneUnits.mry").exists()
                assert file("\$prefix/joinCostFeatures.mry").exists()
                assert file("\$prefix/timeline_basenames.mry").exists()
                assert file("\$prefix/timeline_waveforms.mry").exists()
            }
        }

        task testGenerateVoiceConfig(group: 'Verification') {
            dependsOn generateVoiceConfig
            doLast {
                def configFile = file("\$buildDir/resources/main/marytts/voice/$voiceNameCamelCase/voice.config")
                assert configFile.exists()
                def actual = [:]
                configFile.eachLine { line ->
                    switch(line) {
                        case ~/.+=.+/:
                            def (key, value) = line.split('=', 2)
                            actual[key.trim()] = value.trim()
                            break
                        default:
                            break
                    }
                }
                def expected = [
                        name                                         : "$voiceName",
                        locale                                       : "$voiceLocale",
                        'unitselection.voices.list'                  : "$voiceName",
                        "voice.${voiceName}.acousticModels"          : 'duration F0 midF0 rightF0',
                        "voice.${voiceName}.audioTimelineFile"       : "MARY_BASE/lib/voices/$voiceName/timeline_waveforms.mry",
                        "voice.${voiceName}.audioTimelineReaderClass": 'marytts.unitselection.data.TimelineReader',
                        "voice.${voiceName}.basenameTimeline"        : "MARY_BASE/lib/voices/$voiceName/timeline_basenames.mry",
                        "voice.${voiceName}.cartFile"                : 'jar:/marytts/voice/$voiceNameCamelCase/cart.mry',
                        "voice.${voiceName}.cartReaderClass"         : 'marytts.cart.io.MARYCartReader',
                        "voice.${voiceName}.concatenatorClass"       : 'marytts.unitselection.concat.OverlapUnitConcatenator',
                        "voice.${voiceName}.databaseClass"           : 'marytts.unitselection.data.DiphoneUnitDatabase',
                        "voice.${voiceName}.domain"                  : 'general',
                        "voice.${voiceName}.duration.attribute"      : 'd',
                        "voice.${voiceName}.duration.data"           : "jar:/marytts/voice/$voiceNameCamelCase/dur.tree",
                        "voice.${voiceName}.duration.model"          : 'cart',
                        "voice.${voiceName}.F0.applyTo"              : 'firstVoicedSegments',
                        "voice.${voiceName}.F0.attribute"            : 'f0',
                        "voice.${voiceName}.F0.attribute.format"     : '(0,%.0f)',
                        "voice.${voiceName}.F0.data"                 : "jar:/marytts/voice/$voiceNameCamelCase/f0.left.tree",
                        "voice.${voiceName}.F0.model"                : 'cart',
                        "voice.${voiceName}.F0.predictFrom"          : 'firstVowels',
                        "voice.${voiceName}.featureFile"             : "MARY_BASE/lib/voices/$voiceName/halfphoneFeatures_ac.mry",
                        "voice.${voiceName}.gender"                  : "$voiceGender",
                        "voice.${voiceName}.joinCostClass"           : 'marytts.unitselection.select.JoinCostFeatures',
                        "voice.${voiceName}.joinCostFile"            : "MARY_BASE/lib/voices/$voiceName/joinCostFeatures.mry",
                        "voice.${voiceName}.joinCostWeights"         : 'jar:/marytts/voice/$voiceNameCamelCase/joinCostWeights.txt',
                        "voice.${voiceName}.midF0.applyTo"           : 'firstVowels',
                        "voice.${voiceName}.midF0.attribute"         : 'f0',
                        "voice.${voiceName}.midF0.attribute.format"  : '(50,%.0f)',
                        "voice.${voiceName}.midF0.data"              : "jar:/marytts/voice/$voiceNameCamelCase/f0.mid.tree",
                        "voice.${voiceName}.midF0.model"             : 'cart',
                        "voice.${voiceName}.midF0.predictFrom"       : 'firstVowels',
                        "voice.${voiceName}.locale"                  : "$voiceLocale",
                        "voice.${voiceName}.rightF0.applyTo"         : 'lastVoicedSegments',
                        "voice.${voiceName}.rightF0.attribute"       : 'f0',
                        "voice.${voiceName}.rightF0.attribute.format": '(100,%.0f)',
                        "voice.${voiceName}.rightF0.data"            : "jar:/marytts/voice/$voiceNameCamelCase/f0.right.tree",
                        "voice.${voiceName}.rightF0.model"           : 'cart',
                        "voice.${voiceName}.rightF0.predictFrom"     : 'firstVowels',
                        "voice.${voiceName}.samplingRate"            : '16000',
                        "voice.${voiceName}.selectorClass"           : 'marytts.unitselection.select.DiphoneUnitSelector',
                        "voice.${voiceName}.targetCostClass"         : 'marytts.unitselection.select.DiphoneFFRTargetCostFunction',
                        "voice.${voiceName}.targetCostWeights"       : 'jar:/marytts/voice/$voiceNameCamelCase/halfphoneUnitFeatureDefinition_ac.txt',
                        "voice.${voiceName}.unitReaderClass"         : 'marytts.unitselection.data.UnitFileReader',
                        "voice.${voiceName}.unitsFile"               : "MARY_BASE/lib/voices/$voiceName/halfphoneUnits.mry",
                        "voice.${voiceName}.viterbi.beamsize"        : '100',
                        "voice.${voiceName}.viterbi.wTargetCosts"    : '0.7',
                ]
                assert actual == expected
            }
        }

        import java.util.zip.ZipFile

        task testJar(group: 'Verification') {
            dependsOn jar
            doLast {
                def actual = new ZipFile(jar.archivePath).entries().findAll { !it.isDirectory() }.collect { it.name } as Set
                def expected = [
                    'META-INF/MANIFEST.MF',
                    'META-INF/services/marytts.config.MaryConfig',
                    "META-INF/maven/$group/voice-$voiceName/pom.xml",
                    "META-INF/maven/$group/voice-$voiceName/pom.properties",
                    "marytts/voice/$voiceNameCamelCase/Config.class",
                    "marytts/voice/$voiceNameCamelCase/cart.mry",
                    "marytts/voice/$voiceNameCamelCase/dur.tree",
                    "marytts/voice/$voiceNameCamelCase/f0.left.tree",
                    "marytts/voice/$voiceNameCamelCase/f0.mid.tree",
                    "marytts/voice/$voiceNameCamelCase/f0.right.tree",
                    "marytts/voice/$voiceNameCamelCase/halfphoneUnitFeatureDefinition_ac.txt",
                    "marytts/voice/$voiceNameCamelCase/joinCostWeights.txt",
                    "marytts/voice/$voiceNameCamelCase/voice.config"
                ] as Set
                assert actual == expected
            }
        }

        task testLegacyZip(group: 'Verification') {
            dependsOn legacyZip
            doLast {
                def actual = new ZipFile(legacyZip.archivePath).entries().findAll { !it.isDirectory() }.collect { it.name } as Set
                def expected = [
                    "lib/voices/$voiceName/halfphoneFeatures_ac.mry",
                    "lib/voices/$voiceName/halfphoneUnits.mry",
                    "lib/voices/$voiceName/joinCostFeatures.mry",
                    "lib/voices/$voiceName/timeline_basenames.mry",
                    "lib/voices/$voiceName/timeline_waveforms.mry",
                    "lib/\$jar.archiveName"
                ] as Set
                assert actual == expected
            }
        }
        """

        def expectedLegacyDescriptor = '''<?xml version="1.0"?>
            <marytts-install xmlns="http://mary.dfki.de/installer">
                <voice locale="''' + voiceLocale + '''" name="''' + voiceName + '''" gender="''' + voiceGender + '''" type="unit selection" version="''' + version + '''">
                    <description>A ''' + voiceGender + ''' English unit selection voice</description>
                    <license href="''' + voiceLicenseUrl + '''"/>
                    <package md5sum="$ant.md5Hash" filename="$legacyZip.archiveName" size="${legacyZip.archivePath.size()}">
                        <location folder="true" href="http://mary.dfki.de/download/''' + maryVersion + '''/"/>
                    </package>
                    <depends language="''' + voiceLocale.toLanguageTag() + '''" version="''' + maryVersion + '''"/>
                </voice>
            </marytts-install>'''

        buildFile << """
        import org.custommonkey.xmlunit.XMLUnit

        task testLegacyDescriptor(group: 'Verification') {
            dependsOn legacyDescriptor
            doLast {
                ant.checksum file: legacyZip.archivePath, algorithm: 'MD5', property: 'md5Hash'
                def expected = \"\"\"$expectedLegacyDescriptor\"\"\"
                def actual = legacyDescriptor.destFile.text
                XMLUnit.ignoreWhitespace = true
                assert XMLUnit.compareXML(expected, actual).similar()
            }
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.output
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.output
        assert result.task(':testPlugins').outcome == SUCCESS
    }

    @Test
    void testTemplates() {
        def result = gradle.withArguments('templates').build()
        println result.output
        assert result.task(':templates').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testTemplates').build()
        println result.output
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':testTemplates').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testTemplates'])
    void testLegacyInit() {
        def result = gradle.withArguments('legacyInit').build()
        println result.output
        assert result.task(':lab').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':text').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':wav').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':legacyInit').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyInit').build()
        println result.output
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':testLegacyInit').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPraatPitchmarker() {
        def result = gradle.withArguments('legacyPraatPitchmarker').build()
        println result.output
        assert result.task(':legacyPraatPitchmarker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPraatPitchmarker').build()
        println result.output
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testLegacyPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCEPMaker() {
        def result = gradle.withArguments('legacyMCEPMaker').build()
        println result.output
        assert result.task(':legacyMCEPMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyMCEPMaker').build()
        println result.output
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCEPMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyPhoneUnitLabelComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyHalfPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyTranscriptionAligner() {
        def result = gradle.withArguments('legacyTranscriptionAligner').build()
        println result.output
        assert result.task(':generateAllophones').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':legacyTranscriptionAligner').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyTranscriptionAligner').build()
        println result.output
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':testLegacyTranscriptionAligner').outcome == SUCCESS
    }

    @Test
    void testFeatureLister() {
        def result = gradle.withArguments('featureLister').build()
        println result.output
        assert result.task(':featureLister').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testFeatureLister').build()
        println result.output
        assert result.task(':featureLister').outcome == UP_TO_DATE
        assert result.task(':testFeatureLister').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testFeatureLister'])
    void testPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('phoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':phoneUnitFeatureComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':phoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testFeatureLister'])
    void testHalfPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('halfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':halfPhoneUnitFeatureComputer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testHalfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.task(':halfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testHalfPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyWaveTimelineMaker() {
        def result = gradle.withArguments('legacyWaveTimelineMaker').build()
        println result.output
        assert result.task(':legacyWaveTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyWaveTimelineMaker').build()
        println result.output
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyWaveTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyBasenameTimelineMaker() {
        def result = gradle.withArguments('legacyBasenameTimelineMaker').build()
        println result.output
        assert result.task(':legacyBasenameTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyBasenameTimelineMaker').build()
        println result.output
        assert result.task(':legacyBasenameTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyBasenameTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyMCEPMaker'])
    void testLegacyMCepTimelineMaker() {
        def result = gradle.withArguments('legacyMCepTimelineMaker').build()
        println result.output
        assert result.task(':legacyMCepTimelineMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyMCepTimelineMaker').build()
        println result.output
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCepTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitLabelComputer', 'testPhoneUnitFeatureComputer'])
    void testLegacyPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyPhoneLabelFeatureAligner').build()
        println result.output
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitLabelComputer', 'testHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyHalfPhoneLabelFeatureAligner').build()
        println result.output
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyPhoneUnitLabelComputer', 'testLegacyPhoneLabelFeatureAligner'])
    void testLegacyPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyPhoneUnitfileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyHalfPhoneUnitLabelComputer', 'testLegacyHalfPhoneLabelFeatureAligner'])
    void testLegacyHalfPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitfileWriter', 'testPhoneUnitFeatureComputer'])
    void testLegacyPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyPhoneFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyF0PolynomialFeatureFileWriter() {
        def result = gradle.withArguments('legacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0PolynomialFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyF0PolynomialFeatureFileWriter', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyAcousticFeatureFileWriter() {
        def result = gradle.withArguments('legacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyAcousticFeatureFileWriter').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyAcousticFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyMCepTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyAcousticFeatureFileWriter'])
    void testLegacyJoinCostFileMaker() {
        def result = gradle.withArguments('legacyJoinCostFileMaker').build()
        println result.output
        assert result.task(':legacyJoinCostFileMaker').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyJoinCostFileMaker').build()
        println result.output
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyJoinCostFileMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter'])
    void testLegacyCARTBuilder() {
        def result = gradle.withArguments('legacyCARTBuilder').build()
        println result.output
        assert result.task(':legacyCARTBuilder').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyCARTBuilder').build()
        println result.output
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':testLegacyCARTBuilder').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyDurationCARTTrainer() {
        def result = gradle.withArguments('legacyDurationCARTTrainer').build()
        println result.output
        assert result.task(':legacyDurationCARTTrainer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyDurationCARTTrainer').build()
        println result.output
        assert result.task(':legacyDurationCARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyDurationCARTTrainer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyF0CARTTrainer() {
        def result = gradle.withArguments('legacyF0CARTTrainer').build()
        println result.output
        assert result.task(':legacyF0CARTTrainer').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyF0CARTTrainer').build()
        println result.output
        assert result.task(':legacyF0CARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0CARTTrainer').outcome == SUCCESS
    }

    @Test
    void testGenerateVoiceConfig() {
        def result = gradle.withArguments('generateVoiceConfig').build()
        println result.output
        assert result.task(':generateVoiceConfig').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testGenerateVoiceConfig').build()
        println result.output
        assert result.task(':generateVoiceConfig').outcome == UP_TO_DATE
        assert result.task(':testGenerateVoiceConfig').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyCARTBuilder', 'testGenerateVoiceConfig'])
    void testProcessResources() {
        def result = gradle.withArguments('processResources').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':processResources').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testProcessResources').build()
        println result.output
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':testProcessResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyWaveTimelineMaker', 'testLegacyBasenameTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyPhoneFeatureFileWriter', 'testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyDurationCARTTrainer', 'testLegacyF0CARTTrainer'])
    void testProcessLegacyResources() {
        def result = gradle.withArguments('processLegacyResources').build()
        println result.output
        assert result.task(':processLegacyResources').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testProcessLegacyResources').build()
        println result.output
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':testProcessLegacyResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessResources', 'testProcessLegacyResources'])
    void testIntegrationTest() {
        def result = gradle.withArguments('integrationTest').build()
        println result.output
        assert result.task(':integrationTest').outcome in [SUCCESS, UP_TO_DATE]
    }

    @Test(dependsOnMethods = ['testProcessResources'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.output
        assert result.task(':generatePom').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':generatePomProperties').outcome in [SUCCESS, UP_TO_DATE]
        assert result.task(':jar').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testJar').build()
        println result.output
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        assert result.task(':jar').outcome == UP_TO_DATE
        assert result.task(':testJar').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testJar'])
    void testLegacyZip() {
        def result = gradle.withArguments('legacyZip').build()
        println result.output
        assert result.task(':legacyZip').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyZip').build()
        println result.output
        assert result.task(':legacyZip').outcome == UP_TO_DATE
        assert result.task(':testLegacyZip').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyZip'])
    void testLegacyDescriptor() {
        def result = gradle.withArguments('legacyDescriptor').build()
        println result.output
        assert result.task(':legacyDescriptor').outcome in [SUCCESS, UP_TO_DATE]
        result = gradle.withArguments('testLegacyDescriptor').build()
        println result.output
        assert result.task(':legacyDescriptor').outcome == UP_TO_DATE
        assert result.task(':testLegacyDescriptor').outcome == SUCCESS
    }
}
