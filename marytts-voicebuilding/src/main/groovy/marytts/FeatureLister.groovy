package marytts

import groovy.util.logging.Log4j

import marytts.features.FeatureProcessorManager

@Log4j
class FeatureLister {

    def fpm

    FeatureLister() {
        this(Locale.US)
    }

    FeatureLister(String localeStr) {
        this(Locale.forLanguageTag(localeStr.replaceAll('_', '-')))
    }

    FeatureLister(Locale locale) {
        log.info "locale = $locale"
        fpm = loadFeatureProcessorManager(locale)
    }

    FeatureProcessorManager loadFeatureProcessorManager(Locale locale) {
        try {
            def fpm = Class.forName("marytts.language.${locale.toLanguageTag()}.features.FeatureProcessorManager").newInstance()
            log.info "Instantiated localized FeatureProcessorManager for locale ${locale.toLanguageTag()}"
            return fpm
        } catch (e) {
            log.info "Reflection failed: $e"
        }
        try {
            def fpm = Class.forName("marytts.language.${locale.language}.features.FeatureProcessorManager").newInstance()
            log.info "Instantiated localized FeatureProcessorManager for locale ${locale.language}"
            return fpm
        } catch (e) {
            log.info "Reflection failed: $e"
        }
        try {
            fpm = new FeatureProcessorManager(locale.toLanguageTag())
            log.info "Instantiated generic FeatureProcessorManager for locale ${locale.toLanguageTag()}"
            return fpm
        } catch (e) {
            log.info "Could not instantiate generic FeatureProcessorManager for locale ${locale.toLanguageTag()}"
        }
        try {
            fpm = new FeatureProcessorManager(locale.language)
            log.info "Instantiated generic FeatureProcessorManager for locale ${locale.language}"
            return fpm
        } catch (e) {
            log.info "Could not instantiate generic FeatureProcessorManager for locale ${locale.language}"
        }
        fpm = new FeatureProcessorManager()
        log.warn "Instantiated generic FeatureProcessorManager with no locale!"
        return fpm
    }

    def listFeatures() {
        def byteValuedFeatureNames = fpm.listByteValuedFeatureProcessorNames().tokenize()
        def shortValuedFeatureNames = fpm.listShortValuedFeatureProcessorNames().tokenize()
        byteValuedFeatureNames + shortValuedFeatureNames
    }

    static void main(String[] args) {
        def locale = System.properties.locale
        def lister = new FeatureLister(locale)
        def destFile = new File(System.properties.outputFile)
        try {
            destFile.text = lister.listFeatures().join('\n')
            log.info "Wrote to $destFile"
        } catch (e) {
            log.warn "Could not write to file $destFile": e.message
            println lister.listFeatures().join('\n')
        }
    }
}
