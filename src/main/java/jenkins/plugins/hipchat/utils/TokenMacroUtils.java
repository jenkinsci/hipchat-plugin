package jenkins.plugins.hipchat.utils;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenMacroUtils {

    private static final Logger LOGGER = Logger.getLogger(TokenMacroUtils.class.getName());

    public static ChangeLogSet<? extends Entry> getFirstChangeSet(Run<?, ?> run) {
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> entries;
        try {
            Method method = run.getClass().getMethod("getChangeSets");
            entries = (List<ChangeLogSet<? extends ChangeLogSet.Entry>>) method.invoke(run);
        } catch (ReflectiveOperationException roe) {
            LOGGER.log(Level.WARNING, String.format("Unable to retrieve changesets from Run instance: %s", run),
                    roe);
            entries = Collections.emptyList();
        }
        return entries.isEmpty() ? null : entries.get(0);
    }
}
