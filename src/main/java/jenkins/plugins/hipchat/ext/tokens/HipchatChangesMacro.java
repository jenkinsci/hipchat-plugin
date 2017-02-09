package jenkins.plugins.hipchat.ext.tokens;

import static java.util.logging.Level.*;
import static jenkins.plugins.hipchat.model.Constants.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.Messages;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class HipchatChangesMacro extends TokenMacro {

    private static final Logger LOGGER = Logger.getLogger(HipchatChangesMacro.class.getName());
    private static final List<String> SUPPORTED_TOKENS = ImmutableList.of(HIPCHAT_CHANGES, HIPCHAT_CHANGES_OR_CAUSE);

    @Override
    public boolean acceptsMacroName(String macroName) {
        return SUPPORTED_TOKENS.contains(macroName);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) {
        String changes = null;
        if (!context.hasChangeSetComputed()) {
            LOGGER.log(FINE, "No changeset computed for job {0}", context.getProject().getFullDisplayName());
        } else {
            Set<String> authors = Sets.newHashSet();
            int changedFiles = 0;
            for (Object o : context.getChangeSet().getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
                LOGGER.log(FINEST, "Entry {0}", entry);

                User author = entry.getAuthor();
                if (author == null) {
                    //author may be null in certain cases with git
                    author = User.getUnknown();
                }
                authors.add(author.getDisplayName());
                try {
                    changedFiles += entry.getAffectedFiles().size();
                } catch (UnsupportedOperationException e) {
                    LOGGER.log(INFO, "Unable to collect the affected files for job {0}",
                            context.getProject().getFullDisplayName());
                }
            }
            if (changedFiles == 0 && authors.isEmpty()) {
                LOGGER.log(FINE, "No changes detected");
            } else {
                changes = Messages.StartWithChanges(StringUtils.join(authors, ", "), changedFiles);
            }
        }

        if (HIPCHAT_CHANGES.equals(macroName)) {
            return changes != null ? changes : Messages.NoChanges();
        } else {
            return changes != null ? changes : getCause(context);
        }
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName, Map<String,
            String> arguments, ListMultimap<String, String> argumentMultimap) {
        if (run instanceof AbstractBuild) {
            return evaluate((AbstractBuild<?, ?>) run, listener, macroName, arguments, argumentMultimap);
        }
        return macroName + " is not supported in this context";
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return SUPPORTED_TOKENS;
    }

    private String getCause(AbstractBuild<?, ?> context) {
        CauseAction cause = context.getAction(CauseAction.class);
        if (cause != null) {
            return cause.getShortDescription();
        }
        return "";
    }
}
