package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

    def group = 'de.dfki.mary'
    def voiceName = 'cmu-slt'
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
            license {
                url = "$voiceLicenseUrl"
            }
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', classifier: 'ldom', ext: 'tar.bz2'
        }

        text.srcFileName = 'time.data'

        legacyInit.dependsOn wav, text, lab

        legacyPraatPitchmarker.dependsOn legacyInit

        legacyMCEPMaker.dependsOn legacyInit

        legacyPhoneUnitLabelComputer.dependsOn lab

        legacyHalfPhoneUnitLabelComputer.dependsOn lab

        generateAllophones.dependsOn legacyInit

        legacyWaveTimelineMaker.dependsOn legacyInit

        legacyBasenameTimelineMaker.dependsOn legacyInit

        legacyMCepTimelineMaker.dependsOn legacyInit

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

        task testProcessLegacyResources(group: 'Verification') {
            dependsOn processLegacyResources
            doLast {
                def prefix = "\$sourceSets.main.output.resourcesDir/marytts/voice/\$voice.nameCamelCase"
                assert file("\$prefix/cart.mry").exists()
                assert file("\$prefix/halfphoneUnitFeatureDefinition_ac.txt").exists()
                assert file("\$prefix/joinCostWeights.txt").exists()
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
                    "marytts/voice/\$voice.nameCamelCase/Config.class",
                    "marytts/voice/\$voice.nameCamelCase/cart.mry",
                    "marytts/voice/\$voice.nameCamelCase/halfphoneUnitFeatureDefinition_ac.txt",
                    "marytts/voice/\$voice.nameCamelCase/joinCostWeights.txt",
                    "marytts/voice/\$voice.nameCamelCase/voice.config"
                ] as Set
                assert actual == expected
            }
        }

        task testLegacyZip(group: 'Verification') {
            dependsOn legacyZip
            doLast {
                def actual = new ZipFile(legacyZip.archivePath).entries().findAll { !it.isDirectory() }.collect { it.name } as Set
                def expected = [
                    "lib/voices/$voiceName/dur.tree",
                    "lib/voices/$voiceName/f0.left.tree",
                    "lib/voices/$voiceName/f0.mid.tree",
                    "lib/voices/$voiceName/f0.right.tree",
                    "lib/voices/$voiceName/halfphoneFeatures_ac.mry",
                    "lib/voices/$voiceName/halfphoneUnits.mry",
                    "lib/voices/$voiceName/joinCostFeatures.mry",
                    "lib/voices/$voiceName/phoneUnitFeatureDefinition.txt",
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
                <voice locale="en_US" name="''' + voiceName + '''" gender="female" type="unit selection" version="5.1.1">
                    <description>A female English unit selection voice</description>
                    <license href="''' + voiceLicenseUrl + '''"/>
                    <package md5sum="$ant.md5Hash" filename="$legacyZip.archiveName" size="${legacyZip.archivePath.size()}">
                        <location folder="true" href="http://mary.dfki.de/download/5.1.1/"/>
                    </package>
                    <depends language="en-US" version="5.1.1"/>
                </voice>
            </marytts-install>'''

        buildFile << """
        import org.custommonkey.xmlunit.XMLUnit

        task testLegacyDescriptor(group: 'Verification') {
            dependsOn legacyDescriptor
            doLast {
                ant.checksum file: legacyZip.archivePath, algorithm: 'MD5',  property: 'md5Hash'
                def expected = \"\"\"$expectedLegacyDescriptor\"\"\"
                def actual = file(\"\$project.distsDir/\$project.name-\$project.version-component-descriptor.xml\").text
                XMLUnit.ignoreWhitespace = true
                assert XMLUnit.compareXML(expected, actual).similar()
            }
        }
        """
    }

    @Test
    void testHelp() {
        def result = gradle.withArguments().build()
        println result.standardOutput
        assert result.task(':help').outcome == SUCCESS
    }

    @Test
    void testPlugins() {
        def result = gradle.withArguments('testPlugins').build()
        println result.standardOutput
        assert result.task(':testPlugins').outcome == SUCCESS
    }

    @Test
    void testTemplates() {
        def result = gradle.withArguments('templates').build()
        println result.standardOutput
        assert result.task(':templates').outcome == SUCCESS
        result = gradle.withArguments('testTemplates').build()
        println result.standardOutput
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':testTemplates').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testTemplates'])
    void testLegacyInit() {
        def result = gradle.withArguments('legacyInit').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == SUCCESS
        assert result.task(':text').outcome == SUCCESS
        assert result.task(':lab').outcome == SUCCESS
        assert result.task(':legacyInit').outcome == SUCCESS
        result = gradle.withArguments('testLegacyInit').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':testLegacyInit').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPraatPitchmarker() {
        def result = gradle.withArguments('legacyPraatPitchmarker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPraatPitchmarker').build()
        println result.standardOutput
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':testLegacyPraatPitchmarker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCEPMaker() {
        def result = gradle.withArguments('legacyMCEPMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyMCEPMaker').build()
        println result.standardOutput
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCEPMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyHalfPhoneUnitLabelComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyHalfPhoneUnitLabelComputer').build()
        println result.standardOutput
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitLabelComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyInit'])
    void testLegacyTranscriptionAligner() {
        def result = gradle.withArguments('legacyTranscriptionAligner').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == SUCCESS
        assert result.task(':legacyTranscriptionAligner').outcome == SUCCESS
        result = gradle.withArguments('testLegacyTranscriptionAligner').build()
        println result.standardOutput
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':testLegacyTranscriptionAligner').outcome == SUCCESS
    }

    @Test
    void testLegacyFeatureLister() {
        def result = gradle.withArguments('legacyFeatureLister').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == SUCCESS
        result = gradle.withArguments('testLegacyFeatureLister').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':testLegacyFeatureLister').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner'])
    void testLegacyPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('legacyPhoneUnitFeatureComputer').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        result = gradle.withArguments('testLegacyPhoneUnitFeatureComputer').build()
        println result.standardOutput
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyTranscriptionAligner'])
    void testLegacyHalfPhoneUnitFeatureComputer() {
        def result = gradle.withArguments('legacyHalfPhoneUnitFeatureComputer').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        result = gradle.withArguments('testLegacyHalfPhoneUnitFeatureComputer').build()
        println result.standardOutput
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitFeatureComputer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyWaveTimelineMaker() {
        def result = gradle.withArguments('legacyWaveTimelineMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyWaveTimelineMaker').build()
        println result.standardOutput
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyWaveTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyBasenameTimelineMaker() {
        def result = gradle.withArguments('legacyBasenameTimelineMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyBasenameTimelineMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyBasenameTimelineMaker').build()
        println result.standardOutput
        assert result.task(':legacyBasenameTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyBasenameTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker'])
    void testLegacyMCepTimelineMaker() {
        def result = gradle.withArguments('legacyMCepTimelineMaker').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyMCepTimelineMaker').build()
        println result.standardOutput
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyMCepTimelineMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyPhoneUnitLabelComputer'])
    void testLegacyPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyPhoneUnitfileWriter').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPhoneUnitfileWriter').build()
        println result.standardOutput
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPraatPitchmarker', 'testLegacyHalfPhoneUnitLabelComputer'])
    void testLegacyHalfPhoneUnitfileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneUnitfileWriter').build()
        println result.standardOutput
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyHalfPhoneUnitfileWriter').build()
        println result.standardOutput
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneUnitfileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneUnitfileWriter', 'testLegacyPhoneUnitFeatureComputer'])
    void testLegacyPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyPhoneFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyPhoneFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyHalfPhoneUnitFeatureComputer'])
    void testLegacyHalfPhoneFeatureFileWriter() {
        def result = gradle.withArguments('legacyHalfPhoneFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyHalfPhoneFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyHalfPhoneFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyF0PolynomialFeatureFileWriter() {
        def result = gradle.withArguments('legacyF0PolynomialFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyF0PolynomialFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0PolynomialFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyHalfPhoneUnitfileWriter', 'testLegacyF0PolynomialFeatureFileWriter', 'testLegacyHalfPhoneFeatureFileWriter'])
    void testLegacyAcousticFeatureFileWriter() {
        def result = gradle.withArguments('legacyAcousticFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == SUCCESS
        result = gradle.withArguments('testLegacyAcousticFeatureFileWriter').build()
        println result.standardOutput
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':testLegacyAcousticFeatureFileWriter').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyMCepTimelineMaker', 'testLegacyHalfPhoneUnitfileWriter', 'testLegacyAcousticFeatureFileWriter'])
    void testLegacyJoinCostFileMaker() {
        def result = gradle.withArguments('legacyJoinCostFileMaker').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyJoinCostFileMaker').outcome == SUCCESS
        result = gradle.withArguments('testLegacyJoinCostFileMaker').build()
        println result.standardOutput
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':testLegacyJoinCostFileMaker').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter'])
    void testLegacyCARTBuilder() {
        def result = gradle.withArguments('legacyCARTBuilder').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyCARTBuilder').outcome == SUCCESS
        result = gradle.withArguments('testLegacyCARTBuilder').build()
        println result.standardOutput
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':testLegacyCARTBuilder').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyDurationCARTTrainer() {
        def result = gradle.withArguments('legacyDurationCARTTrainer').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyDurationCARTTrainer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyDurationCARTTrainer').build()
        println result.standardOutput
        assert result.task(':legacyDurationCARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyDurationCARTTrainer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyPhoneFeatureFileWriter', 'testLegacyPhoneUnitfileWriter', 'testLegacyWaveTimelineMaker'])
    void testLegacyF0CARTTrainer() {
        def result = gradle.withArguments('legacyF0CARTTrainer').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0CARTTrainer').outcome == SUCCESS
        result = gradle.withArguments('testLegacyF0CARTTrainer').build()
        println result.standardOutput
        assert result.task(':legacyF0CARTTrainer').outcome == UP_TO_DATE
        assert result.task(':testLegacyF0CARTTrainer').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyAcousticFeatureFileWriter', 'testLegacyJoinCostFileMaker', 'testLegacyCARTBuilder'])
    void testProcessLegacyResources() {
        def result = gradle.withArguments('processLegacyResources').build()
        println result.standardOutput
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':processLegacyResources').outcome == SUCCESS
        result = gradle.withArguments('testProcessLegacyResources').build()
        println result.standardOutput
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':testProcessLegacyResources').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testProcessLegacyResources'])
    void testJar() {
        def result = gradle.withArguments('jar').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == SUCCESS
        assert result.task(':compileJava').outcome == SUCCESS
        assert result.task(':generateServiceLoader').outcome == SUCCESS
        assert result.task(':generateVoiceConfig').outcome == SUCCESS
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':classes').outcome == SUCCESS
        assert result.task(':generatePom').outcome == SUCCESS
        assert result.task(':generatePomProperties').outcome == SUCCESS
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':jar').outcome == SUCCESS
        result = gradle.withArguments('testJar').build()
        println result.standardOutput
        assert result.task(':jar').outcome == UP_TO_DATE
        assert result.task(':testJar').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testJar'])
    void testLegacyZip() {
        def result = gradle.withArguments('legacyZip').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':compileJava').outcome == UP_TO_DATE
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':generateVoiceConfig').outcome == UP_TO_DATE
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':classes').outcome == UP_TO_DATE
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':jar').outcome == UP_TO_DATE
        assert result.task(':legacyBasenameTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyDurationCARTTrainer').outcome == UP_TO_DATE
        assert result.task(':legacyF0CARTTrainer').outcome == UP_TO_DATE
        assert result.task(':legacyZip').outcome == SUCCESS
        result = gradle.withArguments('testlegacyZip').build()
        println result.standardOutput
        assert result.task(':legacyZip').outcome == UP_TO_DATE
        assert result.task(':testLegacyZip').outcome == SUCCESS
    }

    @Test(dependsOnMethods = ['testLegacyZip'])
    void testLegacyDescriptor() {
        def result = gradle.withArguments('legacyDescriptor').build()
        println result.standardOutput
        assert result.task(':generateSource').outcome == UP_TO_DATE
        assert result.task(':compileJava').outcome == UP_TO_DATE
        assert result.task(':generateServiceLoader').outcome == UP_TO_DATE
        assert result.task(':generateVoiceConfig').outcome == UP_TO_DATE
        assert result.task(':processResources').outcome == UP_TO_DATE
        assert result.task(':classes').outcome == UP_TO_DATE
        assert result.task(':generatePom').outcome == UP_TO_DATE
        assert result.task(':generatePomProperties').outcome == UP_TO_DATE
        assert result.task(':legacyFeatureLister').outcome == UP_TO_DATE
        assert result.task(':processDataResources').outcome == UP_TO_DATE
        assert result.task(':lab').outcome == UP_TO_DATE
        assert result.task(':templates').outcome == UP_TO_DATE
        assert result.task(':text').outcome == UP_TO_DATE
        assert result.task(':wav').outcome == UP_TO_DATE
        assert result.task(':legacyInit').outcome == UP_TO_DATE
        assert result.task(':generateAllophones').outcome == UP_TO_DATE
        assert result.task(':legacyTranscriptionAligner').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPraatPitchmarker').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyHalfPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyWaveTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyF0PolynomialFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyAcousticFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyCARTBuilder').outcome == UP_TO_DATE
        assert result.task(':legacyMCEPMaker').outcome == UP_TO_DATE
        assert result.task(':legacyMCepTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyJoinCostFileMaker').outcome == UP_TO_DATE
        assert result.task(':processLegacyResources').outcome == UP_TO_DATE
        assert result.task(':jar').outcome == UP_TO_DATE
        assert result.task(':legacyBasenameTimelineMaker').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitFeatureComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitLabelComputer').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneLabelFeatureAligner').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneUnitfileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyPhoneFeatureFileWriter').outcome == UP_TO_DATE
        assert result.task(':legacyDurationCARTTrainer').outcome == UP_TO_DATE
        assert result.task(':legacyF0CARTTrainer').outcome == UP_TO_DATE
        assert result.task(':legacyZip').outcome == UP_TO_DATE
        assert result.task(':legacyDescriptor').outcome == SUCCESS
        result = gradle.withArguments('testLegacyDescriptor').build()
        println result.standardOutput
        assert result.task(':legacyDescriptor').outcome == UP_TO_DATE
        assert result.task(':testLegacyDescriptor').outcome == SUCCESS
    }
}
