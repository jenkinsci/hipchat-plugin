package jenkins.plugins.hipchat.ext.tokens;

import static java.util.logging.Level.*;
import static jenkins.plugins.hipchat.model.Constants.*;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;

@Extension
public class CommitMessageMacro extends DataBoundTokenMacro {

    private static final Logger LOGGER = Logger.getLogger(CommitMessageMacro.class.getName());

    @Parameter
    public boolean escape = true;

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
        if (!context.hasChangeSetComputed()) {
            LOGGER.log(FINE, "No changeset computed for job {0}", context.getProject().getFullDisplayName());
        } else {
            Object[] items = context.getChangeSet().getItems();
            if (items != null && items.length > 0) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) items[items.length - 1];
                LOGGER.log(FINEST, "Entry {0}", entry);
                return stripMessage(escape ? entry.getMsgEscaped() : entry.getMsg());
            }
        }
        return "";
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) {
        if (run instanceof AbstractBuild) {
            return evaluate((AbstractBuild<?, ?>) run, listener, macroName);
        }
        return macroName + " is not supported in this context";
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return COMMIT_MESSAGE.equals(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(COMMIT_MESSAGE);
    }

    private String stripMessage(String message) {
        return Util.fixNull(message).split("\r?\n")[0];
    }
}
