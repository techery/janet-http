package io.techery.janet;

import java.util.Map;

public class Options {

    public static final String OPTION_FACTORY_CLASS_SUFFIX = "janet.http.factory.class.suffix";
    public final String factoryClassSuffix;

    public Options(Map<String, String> options) {
        this.factoryClassSuffix = options.get(OPTION_FACTORY_CLASS_SUFFIX);
    }
}
