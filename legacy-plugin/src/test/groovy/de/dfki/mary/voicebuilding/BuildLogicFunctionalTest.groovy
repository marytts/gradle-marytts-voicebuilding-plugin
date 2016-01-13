package de.dfki.mary.voicebuilding

import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.*

import static org.gradle.testkit.runner.TaskOutcome.*

class BuildLogicFunctionalTest {

    def gradle
    def buildFile

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
        buildFile << """
        plugins {
            id 'de.dfki.mary.voicebuilding-legacy'
            id 'de.dfki.mary.voicebuilding-festvox'
        }

        dependencies {
            data group: 'org.festvox', name: 'cmu_time_awb', classifier: 'ldom'
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
}
