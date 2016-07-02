package de.dfki.mary.voicebuilding

import org.testng.annotations.*

class VoiceLicenseExtensionTest {

    def license

    @BeforeMethod
    void setup() {
        license = new VoiceLicenseExtension()
    }

    @Test
    void testName() {
        assert license.name == 'Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International'
        license.name = 'foo'
        assert license.name == 'foo'
    }

    @Test
    void testShortName() {
        assert license.shortName == 'CC BY-NC-SA 4.0'
        license.shortName = 'fnord'
        assert license.shortName == 'fnord'
    }

    @Test
    void testUrl() {
        assert license.url == 'http://creativecommons.org/licenses/by-nc-sa/4.0/'
        license.url = 'fnord'
        assert license.url == 'fnord'
    }
}
