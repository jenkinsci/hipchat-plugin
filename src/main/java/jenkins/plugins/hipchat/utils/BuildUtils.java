package jenkins.plugins.hipchat.utils;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.*;
import static java.util.logging.Level.*;

import com.google.common.collect.Sets;
import hudson.EnvVars;
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
        merged.put("URL", jenkins.getRootUrl() + run.getUrl());
        merged.put("CAUSE", cause);
        merged.put("JOB_DISPLAY_NAME", run.getParent().getDisplayName());
        String changes = null;
        if (build != null) {
            changes = getChanges(build);
            merged.put("CHANGES", changes);
        }
        merged.put("CHANGES_OR_CAUSE", changes != null ? changes : cause);

        return merged;
    }

    private static EnvVars getEnvironmentVariables(Run<?, ?> run) {
        try {
            return run.getEnvironment(new LogTaskListener(LOGGER, INFO));
        } catch (IOException e) {
            throw propagate(e);
        } catch (InterruptedException e) {
            throw propagate(e);
        }
    }

    private static String getCause(Run<?, ?> run) {
        CauseAction cause = run.getAction(CauseAction.class);
        if (cause != null) {
            return cause.getShortDescription();
        } else {
            return null;
        }
    }

    private static String getChanges(AbstractBuild<?, ?> build) {
        if (!build.hasChangeSetComputed()) {
            LOGGER.log(FINE, "No changeset computed for job {0}", build.getProject().getFullDisplayName());
            return null;
        }
        Set<String> authors = Sets.newHashSet();
        int changedFiles = 0;
        for (Object o : build.getChangeSet().getItems()) {
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
                        build.getProject().getFullDisplayName());
                return null;
            }
        }
        if (changedFiles == 0) {
            LOGGER.log(FINE, "No changes detected");
            return null;
        }

        return Messages.StartWithChanges(StringUtils.join(authors, ", "), changedFiles);
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
}
