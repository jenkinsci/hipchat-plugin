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
import hudson.scm.ChangeLogSet.Entry;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.utils.TokenMacroUtils;
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
            return getCommitMessage(context.getChangeSet());
        }
        return "";
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) {
        if (run instanceof AbstractBuild) {
            return evaluate((AbstractBuild<?, ?>) run, listener, macroName);
        } else {
            return getCommitMessage(TokenMacroUtils.getFirstChangeSet(run));
        }
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return COMMIT_MESSAGE.equals(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(COMMIT_MESSAGE);
    }

    private String getCommitMessage(ChangeLogSet<? extends Entry> changeSet) {
        if (changeSet != null) {
            Object[] items = changeSet.getItems();
            if (items != null && items.length > 0) {
                Entry entry = (Entry) items[items.length - 1];
                LOGGER.log(FINEST, "Entry {0}", entry);
                return stripMessage(escape ? entry.getMsgEscaped() : entry.getMsg());
            }
        }
        return "";
    }

    private String stripMessage(String message) {
        return Util.fixNull(message).split("\r?\n")[0];
    }
}
