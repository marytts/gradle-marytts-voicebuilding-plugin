package marytts

import groovy.util.logging.Log4j

import marytts.features.FeatureProcessorManager

@Log4j
class FeatureLister {

    def fpm

    FeatureLister() {
        this(Locale.US)
    }

    FeatureLister(Locale locale) {
        fpm = loadFeatureProcessorManager(locale)
    }

    FeatureProcessorManager loadFeatureProcessorManager(Locale locale) {
        try {
            def fpm = Class.forName("marytts.language.${locale}.features.FeatureProcessorManager").newInstance()
            log.info "Instantiated localized FetureProcessorManager for locale ${locale}"
            return fpm
        } catch (e) {
            log.info "Reflection failed: $e"
        }
        try {
            def fpm = Class.forName("marytts.language.${locale.language}.features.FeatureProcessorManager").newInstance()
            log.info "Instantiated localized FetureProcessorManager for locale ${locale.language}"
            return fpm
        } catch (e) {
            log.info "Reflection failed: $e"
        }
        try {
            fpm = new FeatureProcessorManager(locale)
            log.info "Instantiated generic FeatureProcessorManager for locale ${locale}"
            return fpm
        } catch (e) {
            log.info "Could not instantiate generic FeatureProcessorManager for locale ${locale}"
        }
        try {
            fpm = new FeatureProcessorManager(locale.language)
            log.info "Instantiated generic FeatureProcessorManager for locale ${locale.locale}"
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
}
