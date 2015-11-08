package jenkins.plugins.hipchat.utils;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.*;
import static java.util.logging.Level.*;

import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import hudson.model.Result;
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

    public Result findPreviousBuildResult(AbstractBuild<?, ?> build) {
        do {
            build = build.getPreviousBuild();
            if (build == null || build.isBuilding()) {
                return null;
            }
        } while (build.getResult() == Result.ABORTED || build.getResult() == Result.NOT_BUILT);
        return build.getResult();
    }

    public Map<String, String> collectParametersFor(Jenkins jenkins, AbstractBuild<?, ?> build) {
        Map<String, String> merged = newHashMap();
        merged.putAll(build.getBuildVariables());
        merged.putAll(getEnvironmentVariables(build));
        merged.putAll(getTestData(build));

        String cause = getCause(build);
        String changes = getChanges(build);

        merged.put("DURATION", build.getDurationString());
        merged.put("URL", jenkins.getRootUrl() + build.getUrl());
        merged.put("CAUSE", cause);
        merged.put("CHANGES_OR_CAUSE", changes != null ? changes : cause);
        merged.put("CHANGES", changes);
        merged.put("JOB_DISPLAY_NAME", build.getProject().getDisplayName());
        return merged;
    }

    private static EnvVars getEnvironmentVariables(AbstractBuild<?, ?> build) {
        try {
            return build.getEnvironment(new LogTaskListener(LOGGER, INFO));
        } catch (IOException e) {
            throw propagate(e);
        } catch (InterruptedException e) {
            throw propagate(e);
        }
    }

    private static String getCause(AbstractBuild<?, ?> build) {
        CauseAction cause = build.getAction(CauseAction.class);
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

    private Map<String, String> getTestData(AbstractBuild<?, ?> build) {
        Map<String, String> results = newHashMapWithExpectedSize(2);
        AbstractTestResultAction testResults = build.getAction(AbstractTestResultAction.class);
        if (testResults != null) {
            results.put("FAILED_TEST_COUNT", String.valueOf(testResults.getFailCount()));
            results.put("TEST_COUNT", String.valueOf(testResults.getTotalCount()));
        }
        return results;
    }
}
