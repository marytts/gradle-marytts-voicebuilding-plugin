[![Build Status](https://travis-ci.com/marytts/gradle-marytts-voicebuilding-plugin.svg)](https://travis-ci.com/marytts/gradle-marytts-voicebuilding-plugin)
[![Download](https://api.bintray.com/packages/marytts/marytts/gradle-marytts-voicebuilding-plugin/images/download.svg)](https://bintray.com/marytts/marytts/gradle-marytts-voicebuilding-plugin/_latestVersion)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0)

Gradle MaryTTS voicebuilding plugin
===================================

Use this plugin to build new unitselection voices for [MaryTTS].
It's a replacement for the legacy [VoiceImportTools].

Applying the plugin
-------------------

Add this at the top of your `build.gradle` file:
```groovy
plugins {
    id 'de.dfki.mary.voicebuilding-legacy' version '5.4'
}
```
For details, see https://plugins.gradle.org/plugin/de.dfki.mary.voicebuilding-legacy.

Note that Gradle v6.2 or higher is required.

Prerequisites
-------------

### Required third-party software

[Java] 8 or higher is required.

[SoX] and [Edinburgh Speech Tools] must be installed and on the `PATH`.

#### Mac OSX

With [Homebrew], just run

    brew install sox speech-tools

#### Linux

On Debian-based systems, just run

    sudo apt install sox speech-tools

### Project layout

In your project directory, place the source audio, text, and label files under your `build` directory like this:

    build
    ├── lab
    │   ├── utt0001.lab
    │   ├── utt0002.lab
    │   ├── utt0003.lab
    │   ├── utt0004.lab
    │   └── utt0005.lab
    ├── text
    │   ├── utt0001.txt
    │   ├── utt0002.txt
    │   ├── utt0003.txt
    │   ├── utt0004.txt
    │   └── utt0005.txt
    └── resources
        └── data
            ├── utt0001.wav
            ├── utt0002.wav
            ├── utt0003.wav
            ├── utt0004.wav
            └── utt0005.wav

The audio location can be easily customized, by configuring the layout in the `build.gradle` like this:
```groovy
// wav files under "$projectDir/wav"
wav.srcDir = file('wav')
```

Configuring the voice
---------------------

Crucial details of the voice to be built are configured in the `marytts.voice` extension in the `build.gradle` file.
Without any customization, the defaults are:
```groovy
marytts {
    voice {
        name = 'my_voice'
        gender = 'female'
        language = 'en'
        region = 'US'
        domain = 'general'
        type = 'unit selection'
        description = 'A female English unit selection voice'
        samplingRate = 16000
        license {
            name = 'Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International'
            shortName = 'CC BY-NC-SA 4.0'
            url = 'http://creativecommons.org/licenses/by-nc-sa/4.0/'
        }
    }
}
```
Any of these can (and should) be changed as needed, according to the dataset used to build the voice.
The default values do not need to be specified, and the `description` is generated using the values of `gender`, `language`, `region`, and `type`.

Building the voice
------------------

To assemble and test the voice, run

    ./gradlew build

Afterwards, the packaged voice component (and its XML descriptor) will be found under `build/distributions`.
These files can be installed in a MaryTTS v5.2 instance, by placing them in the `download` directory and running the *MaryTTS Component Installer GUI*.

[Edinburgh Speech Tools]: http://www.cstr.ed.ac.uk/projects/speech_tools/
[Homebrew]: https://brew.sh/
[Java]: https://www.java.com/en/download/
[MaryTTS]: http://mary.dfki.de/
[SoX]: http://sox.sourceforge.net/
[VoiceImportTools]: https://github.com/marytts/marytts/wiki/VoiceImportToolsTutorial
