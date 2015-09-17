package marytts

import groovy.util.logging.Log
import groovy.xml.XmlUtil

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType

@Log
class TextToAllophonesConverter {

    LocalMaryInterface mary

    TextToAllophonesConverter() {
        mary = new LocalMaryInterface()
        mary.outputType = MaryDataType.ALLOPHONES
    }

    String convert(String text) {
        def doc = mary.generateXML(text)
        XmlUtil.serialize doc.documentElement
    }

    /**
     * Convert one or more text files into ALLOPHONES-level MaryXML files.
     * @param args pairs of (input, output) files
     */
    static void main(String[] args) {
        def converter = new TextToAllophonesConverter()
        args.collate(2).each { input, output ->
            try {
                def text = new File(input).text
                def xml = convert(text)
                new File(output).text = xml
            } catch (exception) {
                log.warning exception.message
            }
        }
    }
}
