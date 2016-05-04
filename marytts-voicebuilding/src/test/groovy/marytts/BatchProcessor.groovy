package marytts

import groovy.json.JsonSlurper
import groovy.util.logging.Log4j
import groovy.xml.XmlUtil

@Log4j
class BatchProcessor {
    static void main(String[] args) {
        def mary = new LocalMaryInterface()
        args.each { arg ->
            try {
                new File(arg).withReader('UTF-8') { reader ->
                    new JsonSlurper().parse(reader).each { request ->
                        try {
                            def inputText = new File(request.inputFile).getText('UTF-8')
                            mary.inputType = request.inputType
                            mary.outputType = request.outputType
                            def output
                            switch (request.outputType) {
                                case { mary.isTextType(it) }:
                                    output = mary.generateText(inputText)
                                    break
                                case { mary.isXMLType(it) }:
                                    def outputDocument = mary.generateXML(inputText).documentElement
                                    output = XmlUtil.serialize(outputDocument)
                                    break
                                case { mary.isAudioType(it) }:
                                    output = mary.generateAudio(inputText)
                                    break
                                default:
                                    log.error "Cannot process to $it"
                            }
                            log.info output
                            def outputFile = new File(request.outputFile)
                            outputFile.parentFile.mkdirs()
                            outputFile.text = output
                            log.info "Wrote to $outputFile"
                        } catch (e) {
                            log.error e.message
                        }
                    }
                }
            } catch (e) {
                log.error e.message
            }
        }
    }
}
