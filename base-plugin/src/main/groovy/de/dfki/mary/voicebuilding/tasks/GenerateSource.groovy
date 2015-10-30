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
                                'Config.java' """|package marytts.voice.${project.voice.nameCamelCase};
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
                            }
                        }
                    }
                }
            }
        }
    }
}
