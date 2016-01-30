package de.dfki.mary.voicebuilding.tasks

import groovy.util.FileTreeBuilder

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateSource extends DefaultTask {

    @OutputDirectory
    File destDir

    @TaskAction
    void generate() {
        def tree = new FileTreeBuilder(destDir)
        tree {
            main {
                java {
                    marytts {
                        voice {
                            "${project.voice.nameCamelCase}" {
                                'Config.java'(
                                        """|package marytts.voice.${project.voice.nameCamelCase};
                                           |
                                           |import marytts.config.VoiceConfig;
                                           |import marytts.exceptions.MaryConfigurationException;
                                           |
                                           |public class Config extends VoiceConfig {
                                           |    public Config() throws MaryConfigurationException {
                                           |        super(Config.class.getResourceAsStream("voice.config"));
                                           |    }
                                           |}
                                           |""".stripMargin()
                                )
                            }
                        }
                    }
                }
            }
            test {
                java {
                    marytts {
                        voice {
                            "${project.voice.nameCamelCase}" {
                                'ConfigTest.java'(
                                        """|package marytts.voice.${project.voice.nameCamelCase};
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
                                           |    private static final String voiceName = "${project.voice.name}";
                                           |
                                           |    @Test
                                           |    public void isNotMainConfig() throws MaryConfigurationException {
                                           |        MaryConfig m = new Config();
                                           |        assertFalse(m.isMainConfig());
                                           |    }
                                           |
                                           |    @Test
                                           |    public void isVoiceConfig() throws MaryConfigurationException {
                                           |        MaryConfig m = new Config();
                                           |        assertTrue(m.isVoiceConfig());
                                           |    }
                                           |
                                           |    @Test
                                           |    public void hasRightName() throws MaryConfigurationException {
                                           |        VoiceConfig m = new Config();
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
                                )
                            }
                        }
                    }
                }
            }
            integrationTest {
                groovy {
                    marytts {
                        voice {
                            "${project.voice.nameCamelCase}" {
                                'LoadVoiceIT.groovy'(
                                        """|package marytts.voice.${project.voice.nameCamelCase}
                                           |
                                           |import marytts.LocalMaryInterface
                                           |import marytts.datatypes.MaryDataType
                                           |import ${project.voice.type == 'hsmm' ? 'marytts.htsengine.HMMVoice' : 'marytts.unitselection.UnitSelectionVoice'}
                                           |import marytts.util.dom.DomUtils
                                           |
                                           |import org.testng.annotations.*
                                           |
                                           |public class LoadVoiceIT {
                                           |
                                           |    LocalMaryInterface mary
                                           |    Config config
                                           |
                                           |    @BeforeMethod
                                           |    void setup() {
                                           |        mary = new LocalMaryInterface()
                                           |        config = new Config()
                                           |    }
                                           |
                                           |    @Test
                                           |    void canLoadVoice() {
                                           |        def voice = new ${project.voice.type == 'hsmm' ? 'HMM' : 'UnitSelection'}Voice(config.name, null)
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
                                           |        def output = mary.generateText(input)
                                           |        assert output
                                           |    }
                                           |
                                           |    @Test
                                           |    void canProcessTextToSpeech() {
                                           |        def mary = new LocalMaryInterface()
                                           |        def config = new Config()
                                           |        mary.voice = config.name
                                           |        def input = MaryDataType.getExampleText(MaryDataType.TEXT, config.locale)
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
                                           |        def input = DomUtils.parseDocument(example)
                                           |        def output = mary.generateAudio(input)
                                           |        assert output
                                           |    }
                                           |}
                                           |""".stripMargin()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
