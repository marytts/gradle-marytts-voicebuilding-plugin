package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

class GenerateVoiceSource extends DefaultTask {

    @OutputFile
    final RegularFileProperty configTestFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty integrationTestFile = project.objects.fileProperty()

    @TaskAction
    void generate() {

        configTestFile.get().asFile.text =
            """|package marytts.voice.${project.marytts.voice.nameCamelCase};
               |
               |import static org.junit.Assert.*;
               |
               |import marytts.config.MaryConfig;
               |import marytts.config.VoiceConfig;
               |import marytts.exceptions.MaryConfigurationException;
               |
               |import org.junit.Test;
               |
               |/**
               | * @author marc
               | */
               |public class ConfigTest {
               |    private static final String voiceName = "${project.marytts.voice.name}";
               |
               |    @Test
               |    public void isNotMainConfig() throws MaryConfigurationException {
               |        MaryConfig m = new CmuSltConfig();
               |        assertFalse(m.isMainConfig());
               |    }
               |
               |    @Test
               |    public void isVoiceConfig() throws MaryConfigurationException {
               |        MaryConfig m = new CmuSltConfig();
               |        assertTrue(m.isVoiceConfig());
               |    }
               |
               |    @Test
               |    public void hasRightName() throws MaryConfigurationException {
               |        VoiceConfig m = new CmuSltConfig();
               |        assertEquals(voiceName, m.getName());
               |    }
               |
               |    @Test
               |    public void canGetByName() throws MaryConfigurationException {
               |        VoiceConfig m = MaryConfig.getVoiceConfig(voiceName);
               |        assertNotNull(m);
               |        assertEquals(voiceName, m.getName());
               |    }
               |
               |    @Test
               |    public void hasVoiceConfigs() throws MaryConfigurationException {
               |        assertTrue(MaryConfig.countVoiceConfigs() > 0);
               |    }
               |
               |    @Test
               |    public void hasVoiceConfigs2() throws MaryConfigurationException {
               |        Iterable<VoiceConfig> vcs = MaryConfig.getVoiceConfigs();
               |        assertNotNull(vcs);
               |        assertTrue(vcs.iterator().hasNext());
               |    }
               |
               |}
               |""".stripMargin()


        integrationTestFile.get().asFile.text =
            """|package marytts.voice.${project.marytts.voice.nameCamelCase}
               |
               |import marytts.LocalMaryInterface
               |import marytts.datatypes.MaryDataType
               |import ${project.marytts.voice.type == 'hsmm' ? 'marytts.htsengine.HMMVoice' : 'marytts.unitselection.UnitSelectionVoice'}
               |import marytts.util.dom.DomUtils
               |
               |import org.testng.annotations.*
               |
               |public class LoadVoiceIT {
               |
               |    LocalMaryInterface mary
               |    CmuSltConfig config
               |
               |    @BeforeMethod
               |    void setup() {
               |        mary = new LocalMaryInterface()
               |        config = new CmuSltConfig()
               |    }
               |
               |    @Test
               |    void canLoadVoice() {
               |        def voice = new ${project.marytts.voice.type == 'hsmm' ? 'HMM' : 'UnitSelection'}Voice(config.name, null)
               |        assert voice
               |    }
               |
               |    @Test
               |    void canSetVoice() {
               |        mary.voice = config.name
               |        assert config.name == mary.voice
               |    }
               |
               |    @Test
               |    void canProcessTextToTargetfeatures() {
               |        mary.locale = config.locale
               |        mary.outputType = MaryDataType.TARGETFEATURES
               |        def input = MaryDataType.getExampleText(MaryDataType.TEXT, config.locale)
               |        assert input : "Could not get example text for \$MaryDataType.TEXT / locale \$config.locale"
               |        def output = mary.generateText(input)
               |        assert output
               |    }
               |
               |    @Test
               |    void canProcessTextToSpeech() {
               |        def mary = new LocalMaryInterface()
               |        def config = new CmuSltConfig()
               |        mary.voice = config.name
               |        def input = MaryDataType.getExampleText(MaryDataType.TEXT, config.locale)
               |        assert input : "Could not get example text for \$MaryDataType.TEXT / locale \$config.locale"
               |        def output = mary.generateAudio(input)
               |        assert output
               |    }
               |
               |    @Test
               |    void canProcessTokensToTargetfeatures() {
               |        mary.locale = config.locale
               |        mary.inputType = MaryDataType.TOKENS
               |        mary.outputType = MaryDataType.TARGETFEATURES
               |        def example = MaryDataType.getExampleText(MaryDataType.TOKENS, config.locale)
               |        assert example : "Could not get example text for \$MaryDataType.TOKENS / locale \$config.locale"
               |        def input = DomUtils.parseDocument(example)
               |        def output = mary.generateText(input)
               |        assert output
               |    }
               |
               |    @Test
               |    void canProcessTokensToSpeech() {
               |        mary.locale = config.locale
               |        mary.inputType = MaryDataType.TOKENS
               |        def example = MaryDataType.getExampleText(MaryDataType.TOKENS, config.locale)
               |        assert example : "Could not get example text for \$MaryDataType.TOKENS / locale \$config.locale"
               |        def input = DomUtils.parseDocument(example)
               |        def output = mary.generateAudio(input)
               |        assert output
               |    }
               |}
               |""".stripMargin()
    }
}
