package jenkins.plugins.hipchat.utils;

import jenkins.model.Jenkins;

public class GuiceUtils {

    public static <T> T get(Class<T> clazz) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            return jenkins.getInjector().getInstance(clazz);
        }
        throw new IllegalStateException("Jenkins instance is not available");
    }
}
