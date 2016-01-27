package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def group = 'de.dfki.mary'
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

        voice {
            name = "$voiceName"
            gender = "$voiceGender"
            region = "$voiceLocale.country"
            license {
                url = "$voiceLicenseUrl"
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
                assert fileTree(buildDir).include('templates/*.java').files
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

        task testLegacyFeatureLister(group: 'Verification') {
            dependsOn legacyFeatureLister
            doLast {
                assert file("\$buildDir/mary/features.txt").exists()
            }
        }

        task testLegacyPhoneUnitFeatureComputer(group: 'Verification') {
            dependsOn legacyPhoneUnitFeatureComputer
            doLast {
                assert fileTree(buildDir).include('phonefeatures/*.pfeats').files
            }
        }

        task testLegacyHalfPhoneUnitFeatureComputer(group: 'Verification') {
            dependsOn legacyHalfPhoneUnitFeatureComputer
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
                    "META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.xml",
                    "META-INF/maven/${group.replace '.', '/'}/voice-$voiceName/pom.properties",
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
                <voice locale="''' + voiceLocale + '''" name="''' + voiceName + '''" gender="''' + voiceGender + '''" type="unit selection" version="5.1.1">
                    <description>A ''' + voiceGender + ''' English unit selection voice</description>
                    <license href="''' + voiceLicenseUrl + '''"/>
                    <package md5sum="$ant.md5Hash" filename="$legacyZip.archiveName" size="${legacyZip.archivePath.size()}">
                        <location folder="true" href="http://mary.dfki.de/download/5.1.1/"/>
                    </package>
                    <depends language="''' + voiceLocale.toLanguageTag() + '''" version="5.1.1"/>
                </voice>
            </marytts-install>'''

        buildFile << """
        import org.custommonkey.xmlunit.XMLUnit

        task testLegacyDescriptor(group: 'Verification') {
            dependsOn legacyDescriptor
            doLast {
                ant.checksum file: legacyZip.archivePath, algorithm: 'MD5', property: 'md5Hash'
                def expected = \"\"\"$expectedLegacyDescriptor\"\"\"
                def actual = file(\"\$distsDir/\${legacyZip.archiveName.replace('.zip', '-component-descriptor.xml')}\").text
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
        assert result.taskPaths(SUCCESS) == [':help']
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testPlugins']
    }

    @Test
    void testTemplates() {
        def result = gradle.withArguments('templates').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':templates']
        result = gradle.withArguments('testTemplates').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testTemplates']
    }

    @Test(dependsOnMethods = ['testTemplates'])
    void testLegacyInit() {
        def result = gradle.withArguments('legacyInit').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':lab', ':text', ':wav', ':legacyInit']
        result = gradle.withArguments('testLegacyInit').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyInit']
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPraatPitchmarker() {
        def result = gradle.withArguments('legacyPraatPitchmarker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPraatPitchmarker']
        result = gradle.withArguments('testLegacyPraatPitchmarker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyPraatPitchmarker']
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCEPMaker() {
        def result = gradle.withArguments('legacyMCEPMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyMCEPMaker']
        result = gradle.withArguments('testLegacyMCEPMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyMCEPMaker']
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPhoneUnitLabelComputer']
        result = gradle.withArguments('testLegacyPhoneUnitLabelComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyPhoneUnitLabelComputer']
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyHalfPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyHalfPhoneUnitLabelComputer']
        result = gradle.withArguments('testLegacyHalfPhoneUnitLabelComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyHalfPhoneUnitLabelComputer']
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyTranscriptionAligner() {
        def result = gradle.withArguments('legacyTranscriptionAligner').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateAllophones', ':legacyTranscriptionAligner']
        result = gradle.withArguments('testLegacyTranscriptionAligner').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyTranscriptionAligner']
    }

    @Test
    void testLegacyFeatureLister() {
        def result = gradle.withArguments('legacyFeatureLister').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyFeatureLister']
        result = gradle.withArguments('testLegacyFeatureLister').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyFeatureLister']
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testLegacyFeatureLister'])
    void testLegacyPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('legacyPhoneUnitFeatureComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPhoneUnitFeatureComputer']
        result = gradle.withArguments('testLegacyPhoneUnitFeatureComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyPhoneUnitFeatureComputer']
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner', 'testLegacyFeatureLister'])
    void testLegacyHalfPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyHalfPhoneUnitFeatureComputer']
        result = gradle.withArguments('testLegacyHalfPhoneUnitFeatureComputer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyHalfPhoneUnitFeatureComputer']
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyWaveTimelineMaker() {
        def result = gradle.withArguments('legacyWaveTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyWaveTimelineMaker']
        result = gradle.withArguments('testLegacyWaveTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyWaveTimelineMaker']
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyBasenameTimelineMaker() {
        def result = gradle.withArguments('legacyBasenameTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyBasenameTimelineMaker']
        result = gradle.withArguments('testLegacyBasenameTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyBasenameTimelineMaker']
    }

    @Test(dependsOnMethods = ['testLegacyMCEPMaker'])
    void testLegacyMCepTimelineMaker() {
        def result = gradle.withArguments('legacyMCepTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyMCepTimelineMaker']
        result = gradle.withArguments('testLegacyMCepTimelineMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyMCepTimelineMaker']
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitLabelComputer', 'testLegacyPhoneUnitFeatureComputer'])
    void testLegacyPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyPhoneLabelFeatureAligner').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPhoneLabelFeatureAligner']
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitLabelComputer', 'testLegacyHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneLabelFeatureAligner() {
        def result = gradle.withArguments('legacyHalfPhoneLabelFeatureAligner').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyHalfPhoneLabelFeatureAligner']
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyPhoneUnitLabelComputer', 'testLegacyPhoneLabelFeatureAligner'])
    void testLegacyPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyPhoneUnitfileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPhoneUnitfileWriter']
        result = gradle.withArguments('testLegacyPhoneUnitfileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyPhoneUnitfileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyHalfPhoneUnitLabelComputer', 'testLegacyHalfPhoneLabelFeatureAligner'])
    void testLegacyHalfPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyHalfPhoneUnitfileWriter']
        result = gradle.withArguments('testLegacyHalfPhoneUnitfileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyHalfPhoneUnitfileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitfileWriter', 'testLegacyPhoneUnitFeatureComputer'])
    void testLegacyPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyPhoneFeatureFileWriter']
        result = gradle.withArguments('testLegacyPhoneFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyPhoneFeatureFileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyHalfPhoneFeatureFileWriter']
        result = gradle.withArguments('testLegacyHalfPhoneFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyHalfPhoneFeatureFileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyF0PolynomialFeatureFileWriter() {
        def result = gradle.withArguments('legacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyF0PolynomialFeatureFileWriter']
        result = gradle.withArguments('testLegacyF0PolynomialFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyF0PolynomialFeatureFileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyF0PolynomialFeatureFileWriter', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyAcousticFeatureFileWriter() {
        def result = gradle.withArguments('legacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyAcousticFeatureFileWriter']
        result = gradle.withArguments('testLegacyAcousticFeatureFileWriter').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyAcousticFeatureFileWriter']
    }

    @Test(dependsOnMethods = ['testLegacyMCepTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyAcousticFeatureFileWriter'])
    void testLegacyJoinCostFileMaker() {
        def result = gradle.withArguments('legacyJoinCostFileMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyJoinCostFileMaker']
        result = gradle.withArguments('testLegacyJoinCostFileMaker').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyJoinCostFileMaker']
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter'])
    void testLegacyCARTBuilder() {
        def result = gradle.withArguments('legacyCARTBuilder').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyCARTBuilder']
        result = gradle.withArguments('testLegacyCARTBuilder').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyCARTBuilder']
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyDurationCARTTrainer() {
        def result = gradle.withArguments('legacyDurationCARTTrainer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyDurationCARTTrainer']
        result = gradle.withArguments('testLegacyDurationCARTTrainer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyDurationCARTTrainer']
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyF0CARTTrainer() {
        def result = gradle.withArguments('legacyF0CARTTrainer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyF0CARTTrainer']
        result = gradle.withArguments('testLegacyF0CARTTrainer').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyF0CARTTrainer']
    }

    @Test
    void testGenerateVoiceConfig() {
        def result = gradle.withArguments('generateVoiceConfig').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateVoiceConfig']
        result = gradle.withArguments('testGenerateVoiceConfig').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testGenerateVoiceConfig']
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyCARTBuilder', 'testGenerateVoiceConfig'])
    void testProcessResources() {
        def result = gradle.withArguments('processResources').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generateServiceLoader', ':processResources']
        result = gradle.withArguments('testProcessResources').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testProcessResources']
    }

    @Test(dependsOnMethods = ['testLegacyWaveTimelineMaker', 'testLegacyBasenameTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyPhoneFeatureFileWriter', 'testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyDurationCARTTrainer', 'testLegacyF0CARTTrainer'])
    void testProcessLegacyResources() {
        def result = gradle.withArguments('processLegacyResources').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':processLegacyResources']
        result = gradle.withArguments('testProcessLegacyResources').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testProcessLegacyResources']
    }

    @Test(dependsOnMethods = ['testProcessResources', 'testProcessLegacyResources'])
    void testIntegrationTest() {
        def result = gradle.withArguments('integrationTest').build()
        println result.output
        assert result.taskPaths(SUCCESS).contains(':integrationTest')
    }

    @Test(dependsOnMethods = ['testProcessResources'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':generatePom', ':generatePomProperties', ':jar']
        result = gradle.withArguments('testJar').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testJar']
    }

    @Test(dependsOnMethods = ['testJar'])
    void testLegacyZip() {
        def result = gradle.withArguments('legacyZip').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyZip']
        result = gradle.withArguments('testLegacyZip').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyZip']
    }

    @Test(dependsOnMethods = ['testLegacyZip'])
    void testLegacyDescriptor() {
        def result = gradle.withArguments('legacyDescriptor').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':legacyDescriptor']
        result = gradle.withArguments('testLegacyDescriptor').build()
        println result.output
        assert result.taskPaths(SUCCESS) == [':testLegacyDescriptor']
    }
}
