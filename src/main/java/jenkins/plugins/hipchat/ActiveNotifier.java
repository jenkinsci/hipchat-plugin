package jenkins.plugins.hipchat;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.CauseAction;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

    HipChatNotifier notifier;

    public ActiveNotifier(HipChatNotifier notifier) {
        super();
        this.notifier = notifier;
    }

    private HipChatService getHipChat(AbstractBuild r) {
        AbstractProject<?, ?> project = r.getProject();
        String projectRoom = Util.fixEmpty(project.getProperty(HipChatNotifier.HipChatJobProperty.class).getRoom());
        return notifier.newHipChatService(projectRoom);
    }

    public void deleted(AbstractBuild r) {

    }

    public void started(AbstractBuild build) {
        String changes = getChanges(build);
        CauseAction cause = build.getAction(CauseAction.class);

        if (changes != null) {
            notifyStart(build, changes);
        } else if (cause != null) {
            MessageBuilder message = new MessageBuilder(notifier, build);
            message.append(cause.getShortDescription());
            notifyStart(build, message.appendOpenLink().toString());
        } else {
            notifyStart(build, getBuildStatusMessage(build));
        }
    }

    private void notifyStart(AbstractBuild build, String message) {
        getHipChat(build).publish(message, "yellow");
    }

    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {

        AbstractProject<?, ?> project = r.getProject();
        HipChatNotifier.HipChatJobProperty jobProperty = project.getProperty(HipChatNotifier.HipChatJobProperty.class);
        Result result = r.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild().getPreviousBuild();
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;

        if (result != null && jobProperty != null && ((result == Result.ABORTED && jobProperty.getNotifyAborted())
                || (result == Result.FAILURE && jobProperty.getNotifyFailure())
                || (result == Result.NOT_BUILT && jobProperty.getNotifyNotBuilt())
                || (result == Result.SUCCESS && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE) && jobProperty.getNotifyBackToNormal())
                || (result == Result.SUCCESS && jobProperty.getNotifySuccess())
                || (result == Result.UNSTABLE && jobProperty.getNotifyUnstable()))) {

            getHipChat(r).publish(getBuildStatusMessage(r), getBuildColor(r));
        }

    }

    String getChanges(AbstractBuild r) {
        if (!r.hasChangeSetComputed()) {
            logger.info("No change set computed...");
            return null;
        }
        ChangeLogSet changeSet = r.getChangeSet();
        List<Entry> entries = new LinkedList<Entry>();
        Set<AffectedFile> files = new HashSet<AffectedFile>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
            files.addAll(entry.getAffectedFiles());
        }
        if (entries.isEmpty()) {
            logger.info("Empty change...");
            return null;
        }
        Set<String> authors = new HashSet<String>();
        for (Entry entry : entries) {
            authors.add(entry.getAuthor().getDisplayName());
        }
        MessageBuilder message = new MessageBuilder(notifier, r);
        message.append("Started by changes from ");
        message.append(StringUtils.join(authors, ", "));
        message.append(" (");
        message.append(files.size());
        message.append(" file(s) changed)");
        return message.appendOpenLink().toString();
    }

    String getBuildColor(AbstractBuild r) {
        Result result = r.getResult();
        if (result == Result.SUCCESS) {
            return "green";
        } else if (result == Result.FAILURE) {
            return "red";
        } else if (result == Result.UNSTABLE && !remainCalmUnstable(r)) {
            return "red";
        } else {
            return "yellow";
        }
    }

    boolean remainCalmUnstable(AbstractBuild r) {

        boolean remainCalm = true;

        // Get the job property
        AbstractProject<?, ?> project = r.getProject();
        HipChatNotifier.HipChatJobProperty jobProperty = project.getProperty(HipChatNotifier.HipChatJobProperty.class);

        // If there exists associated aggregated result action
        if (r.getAction(TestResultAction.class) != null) {
            TestResult tr = r.getAction(TestResultAction.class).getResult();

            logger.info("Number of Failed Cases: " + Integer.toString(tr.getFailCount()));
            for (CaseResult res : tr.getFailedTests()) {
                if (res.getAge() > jobProperty.getCalmUnstableAgeLimit()) {
                    remainCalm = false;
                    break;
                }
            }
        } else { // If none test result is associated
            logger.info("There is no associated test result with current build!");
            remainCalm = true;
        }
        return remainCalm;
    }

    String getBuildStatusMessage(AbstractBuild r) {
        MessageBuilder message = new MessageBuilder(notifier, r);
        message.appendStatusMessage();
        message.appendDuration();
        return message.appendOpenLink().toString();
    }

    public static class MessageBuilder {
        private StringBuffer message;
        private HipChatNotifier notifier;
        private AbstractBuild build;

        public MessageBuilder(HipChatNotifier notifier, AbstractBuild build) {
            this.notifier = notifier;
            this.message = new StringBuffer();
            this.build = build;
            startMessage();
        }

        public MessageBuilder appendStatusMessage() {
            message.append(getStatusMessage(build));
            return this;
        }

        static String getStatusMessage(AbstractBuild r) {
            if (r.isBuilding()) {
                return "Starting...";
            }
            Result result = r.getResult();
            Run previousBuild = r.getProject().getLastBuild().getPreviousBuild();
            Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
            if (result == Result.SUCCESS && previousResult == Result.FAILURE) return "Back to normal";
            if (result == Result.SUCCESS) return "Success";
            if (result == Result.FAILURE) return "<b>FAILURE</b>";
            if (result == Result.ABORTED) return "ABORTED";
            if (result == Result.NOT_BUILT) return "Not built";
            if (result == Result.UNSTABLE) return "Unstable";
            return "Unknown";
        }

        public MessageBuilder append(String string) {
            message.append(string);
            return this;
        }

        public MessageBuilder append(Object string) {
            message.append(string.toString());
            return this;
        }

        private MessageBuilder startMessage() {
            message.append(build.getProject().getDisplayName());
            message.append(" - ");
            message.append(build.getDisplayName());
            message.append(" ");
            if (build.getProject().isParameterized()) {
                message.append("[");
                List<String> params = new LinkedList<String>();
                for (ParametersAction parametersAction : build.getActions(ParametersAction.class)) {
                    for (ParameterValue parameterValue : parametersAction.getParameters()) {
                        String name = parameterValue.getName();
                        String value = parameterValue.createVariableResolver(build).resolve(name);
                        params.add(name + '=' + value);
                    }
                }
                message.append(StringUtils.join(params, ", "));
                message.append("] ");
            }
            return this;
        }

        public MessageBuilder appendOpenLink() {
            String url = notifier.getBuildServerUrl() + build.getUrl();
            message.append(" (<a href='").append(url).append("'>Open</a>)").append(" (<a href='").append(url)
                    .append("console").append("'>Console</a>)");
            return this;
        }

        // TODO decide if we need this...
        public MessageBuilder appendCommitInfo() {
            ChangeLogSet changeSet = build.getChangeSet();
            if (!changeSet.isEmptySet()) {
                List<Entry> entries = new LinkedList<Entry>();
                for (Object o : changeSet.getItems()) {
                    Entry entry = (Entry) o;
                    entries.add(entry);
                }
                String commitAuthor = entries.get((entries.size() - 1)).getAuthor().getDisplayName();
                String commitMsg = entries.get((entries.size() - 1)).getMsgEscaped();
                String commitId = entries.get((entries.size() - 1)).getCommitId();
                message.append(commitAuthor);
                message.append(": ");
                message.append(commitMsg);
                message.append(" (");
                message.append(commitId.substring(0, 6));
                message.append(")");
            }
            return this;
        }

        public MessageBuilder appendDuration() {
            message.append(" after ");
            message.append(build.getDurationString());
            return this;
        }

        public String toString() {
            return message.toString();
        }
    }
}
