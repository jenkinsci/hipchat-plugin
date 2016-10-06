package jenkins.plugins.hipchat.utils;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.*;
import static java.util.logging.Level.*;

import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.LogTaskListener;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Singleton;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.Messages;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

@Singleton
public class BuildUtils {

    private static final Logger LOGGER = Logger.getLogger(BuildUtils.class.getName());

    public Result findPreviousBuildResult(Run<?, ?> run) {
        do {
            run = run.getPreviousBuild();
            if (run == null || run.isBuilding()) {
                return null;
            }
        } while (run.getResult() == Result.ABORTED || run.getResult() == Result.NOT_BUILT);
        return run.getResult();
    }

    public Map<String, String> collectParametersFor(Jenkins jenkins, Run<?, ?> run) {
        Map<String, String> merged = newHashMap();
        AbstractBuild<?, ?> build = null;
        if (run instanceof AbstractBuild) {
            build = (AbstractBuild<?, ?>) run;
            merged.putAll(build.getBuildVariables());
        }
        merged.putAll(getEnvironmentVariables(run));
        merged.putAll(getTestData(run));
        String cause = getCause(run);
        merged.put("DURATION", run.getDurationString());
        merged.put("URL", DisplayURLProvider.get().getRunURL(run));
        merged.put("CAUSE", cause);
        merged.put("JOB_DISPLAY_NAME", run.getParent().getDisplayName());
        merged.putAll(getChangeSetData(build, cause));

        return merged;
    }

    private EnvVars getEnvironmentVariables(Run<?, ?> run) {
        try {
            return run.getEnvironment(new LogTaskListener(LOGGER, INFO));
        } catch (IOException e) {
            throw propagate(e);
        } catch (InterruptedException e) {
            throw propagate(e);
        }
    }

    private String getCause(Run<?, ?> run) {
        CauseAction cause = run.getAction(CauseAction.class);
        if (cause != null) {
            return cause.getShortDescription();
        } else {
            return null;
        }
    }

    private Map<String, String> getChangeSetData(AbstractBuild<?, ?> build, String cause) {
        String changes = null;
        String commitMessage = "";
        String commitMessageText = "";
        Map<String, String> ret = newHashMapWithExpectedSize(2);
        if (build != null) {
            if (!build.hasChangeSetComputed()) {
                LOGGER.log(FINE, "No changeset computed for job {0}", build.getProject().getFullDisplayName());
            } else {
                Set<String> authors = Sets.newHashSet();
                int changedFiles = 0;
                for (Object o : build.getChangeSet().getItems()) {
                    ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
                    LOGGER.log(FINEST, "Entry {0}", entry);
                    commitMessage = stripMessage(entry.getMsgEscaped());
                    commitMessageText = stripMessage(entry.getMsg());

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
                                build.getProject().getFullDisplayName());
                        return null;
                    }
                }
                if (changedFiles == 0) {
                    LOGGER.log(FINE, "No changes detected");
                } else {
                    changes = Messages.StartWithChanges(StringUtils.join(authors, ", "), changedFiles);
                }
            }
        }

        ret.put("COMMIT_MESSAGE", commitMessage);
        ret.put("COMMIT_MESSAGE_TEXT", commitMessageText);
        ret.put("CHANGES", changes != null ? changes : Messages.NoChanges());
        ret.put("CHANGES_OR_CAUSE", changes != null ? changes : cause);
        return ret;
    }

    private Map<String, String> getTestData(Run<?, ?> run) {
        Map<String, String> results = newHashMapWithExpectedSize(2);
        AbstractTestResultAction testResults = run.getAction(AbstractTestResultAction.class);
        if (testResults != null) {
            results.put("FAILED_TEST_COUNT", String.valueOf(testResults.getFailCount()));
            results.put("TEST_COUNT", String.valueOf(testResults.getTotalCount()));
        }
        return results;
    }

    private String stripMessage(String message) {
        return Util.fixNull(message).split("\r?\n")[0];
    }
}
