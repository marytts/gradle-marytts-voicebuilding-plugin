Gradle MaryTTS voicebuilding plugin
===================================

[Unreleased]
------------

### Added

- Plugin usage in readme

### Changed

- [all changes since v5.4]

[v5.4] - 2018-12-12
-------------------

### Added

- Optionally specify utterances to include or exclude

### Changed

- Rewritten, functionally equivalent build logic;
  parallel processing for ~6-fold increase in processing efficiency
- One-shot build via lazy configuration (no need to run `legacyInit` first)
- Data processing controlled by `basenames.lst`, not file globs
- Legacy artifacts built (and published) by default
- Run functional tests in parallel
- Build with Gradle v4.10.2
- Drop support for Java 7
- [all changes since v5.3.2]

### Fixed

- BufferUnderflowExceptions after building timelines with mismatched non-speech units
- Exclude utterances with missing text/lab files
- Concurrent Praat unpacking errors on Mac OSX by upgrading wrapper plugin

[v5.3.2] - 2018-01-26
---------------------

### Changed

- [all changes since v5.3.1]

### Fixed

- Prevent transitive Groovy dependency from leaking into classpath of consuming projects (properly this time!)

[v5.3.1] - 2018-01-26
---------------------

### Changed

- [all changes since v5.3]

### Fixed

- Prevent transitive Groovy dependency from leaking into classpath of consuming projects (turned out not to have been the proper solution...)

[v5.3] - 2017-11-21
-------------------

### Added

- Parallel data processing using Gradle Worker API
- Use Praat binary provided by [Gradle plugin](https://github.com/m2ci-msp/gradle-praat-wrapper-plugin)
- Snapshots published to [OJO](https://oss.jfrog.org/)

### Changed

- Build with Gradle v4.3.1
- Rewrote several task classes used by data plugin
- Fixed Travis CI integration to work with "trusty" containers
- [all changes since v5.2.2]

[v5.2.2] - 2017-07-21
---------------------

### Changed

- Build with Gradle v3.5
- Parameterized functional tests via buildscript resources
- [all changes since v5.2.1]

### Removed

- Excluded transitive `groovy-all` dependency
- Bundled helper library split into separate artifact on JCenter

[v5.2.1] - 2017-04-13
---------------------

### Added

- Plugin publishing via Gradle Plugin Publishing plugin

### Changed

- [all changes since v5.2.0]

[v5.2.0] - 2016-10-13
---------------------

### Changed

- Upgrade MaryTTS to v5.2
- Build with Gradle v2.14.1
- Configurable language component dependency
- Split into four distinct plugins
- [all changes since v0.5.2.1]

### Added

- CI testing via Travis
- Plugin functional testing via Gradle TestKit
- Bundled helper library code for batch processing

[v0.5.2.1] - 2015-03-05
-----------------------

### Changed

- Switched to GPL
- Build with Gradle v2.3
- [all changes since v0.5.1.2]

[v0.5.1.2] - 2015-02-09
-----------------------

### Changed

- Upgrade MaryTTS v5.1.2
- Build with Gradle v2.2.1
- [all changes since v0.5.1]

[v0.5.1] - 2014-10-07
---------------------

### Initial release

- Initial version indexed on plugins.gradle.org, extracted from project used to build voices for MaryTTS v5.1

[Unreleased]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/tree/master
[all changes since v5.4]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.4...HEAD
[v5.4]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.4
[all changes since v5.3.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.3.2...v5.4
[v5.3.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.3.2
[all changes since v5.3.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.3.1...v5.3.2
[v5.3.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.3.1
[all changes since v5.3]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.3...v5.3.1
[v5.3]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.3
[all changes since v5.2.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.2.2...v5.3
[v5.2.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.2.2
[all changes since v5.2.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.2.1...v5.2.2
[v5.2.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.2.1
[all changes since v5.2.0]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v5.2.0...v5.2.1
[v5.2.0]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v5.2.0
[all changes since v0.5.2.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v0.5.2.1...v5.2.0
[v0.5.2.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v0.5.2.1
[all changes since v0.5.1.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v0.5.1.2...v0.5.2.1
[v0.5.1.2]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v0.5.1.2
[all changes since v0.5.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/compare/v0.5.1...v0.5.1.2
[v0.5.1]: https://github.com/marytts/gradle-marytts-voicebuilding-plugin/releases/tag/v0.5.1
