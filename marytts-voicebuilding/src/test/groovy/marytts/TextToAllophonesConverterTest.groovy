package marytts

import org.custommonkey.xmlunit.*

import org.testng.annotations.*

class TextToAllophonesConverterTest {

    def converter

    @BeforeTest
    void setup() {
        converter = new TextToAllophonesConverter()
        XMLUnit.ignoreWhitespace = true
    }

    @DataProvider
    Object[][] data() {
        [['hello',
          '''<?xml version="1.0" encoding="UTF-8"?>
            <maryxml xmlns="http://mary.dfki.de/2002/MaryXML" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="0.5"
         xml:lang="en-US">
            <p>
                <s>
                    <phrase>
                        <t accent="!H*" g2p_method="lexicon" ph="h @ - ' l @U" pos="UH">
                            hello
                            <syllable ph="h @">
                                <ph p="h"/>
                                <ph p="@"/>
                            </syllable>
                            <syllable accent="!H*" ph="l @U" stress="1">
                                <ph p="l"/>
                                <ph p="@U"/>
                            </syllable>
                        </t>
                        <boundary breakindex="5" tone="L-L%"/>
                    </phrase>
                </s>
            </p>
        </maryxml>
        ''']]
    }

    @Test(dataProvider = 'data')
    void testConvert(text, expected) {
        def actual = converter.convert(text)
        def comparison = XMLUnit.compareXML(expected, actual)
        def data = new DetailedDiff(comparison)
        assert data.similar()
    }
}
