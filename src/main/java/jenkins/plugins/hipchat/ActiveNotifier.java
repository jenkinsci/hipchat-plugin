package jenkins.plugins.hipchat;

import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

    private final HipChatNotifier notifier;

    public ActiveNotifier(HipChatNotifier notifier) {
        super();
        this.notifier = notifier;
    }

    private HipChatService getHipChat() {
        return notifier.newHipChatService();
    }

    public void deleted(AbstractBuild r) {
    }

    public void started(AbstractBuild build) {
        String changes = getChanges(build);
        CauseAction cause = build.getAction(CauseAction.class);

        if (changes != null) {
            notifyStart(build, changes);
        } else if (cause != null) {
            MessageBuilder message = new MessageBuilder(build);
            message.append(cause.getShortDescription());
            notifyStart(build, message.appendOpenLink().toString());
        } else {
            notifyStart(build, getBuildStatusMessage(build, null));
        }
    }

    private void notifyStart(AbstractBuild build, String message) {
        getHipChat().publish(message, "green");
    }

    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild r) {
        AbstractProject<?, ?> project = r.getProject();
        Result result = r.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild().getPreviousBuild();
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
        if ((result == Result.ABORTED && notifier.isNotifyAborted())
                || (result == Result.FAILURE && notifier.isNotifyFailure())
                || (result == Result.NOT_BUILT && notifier.isNotifyNotBuilt())
                || (result == Result.SUCCESS && previousResult == Result.FAILURE && notifier.isNotifyBackToNormal())
                || (result == Result.SUCCESS && notifier.isNotifySuccess())
                || (result == Result.UNSTABLE && notifier.isNotifyUnstable())) {
            getHipChat().publish(getBuildStatusMessage(r, fetchCulpritsIfWanted(r)), getBuildColor(r));
        }
    }

    private List<String> fetchCulpritsIfWanted(AbstractBuild r) {
        if (notifier.isIncludeCulprits()) {
            return getCulpritsInHipchat(r);
        } else {
            return null;
        }
    }

    private List<String> getCulpritsInHipchat(AbstractBuild r) {
        List<String> hipchatUsernames = new ArrayList<String>();
        for(Object userObj : r.getCulprits()) {
            User user = (User)userObj;
            Mailer.UserProperty mailProperty = (user).getProperty(Mailer.UserProperty.class);
            logger.log(Level.SEVERE, "Getting email for "+user.getFullName());
            if(mailProperty != null && !StringUtils.isEmpty(mailProperty.getAddress())) {
                logger.log(Level.SEVERE, "It's "+mailProperty.getAddress());
                hipchatUsernames.add("@"+getHipChat().getMentionNameForEmail(mailProperty.getAddress()));
            }else{
                hipchatUsernames.add(user.getFullName());
            }
        }
        return hipchatUsernames;
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
            logger.log(Level.INFO, "Entry {0}", o);
            entries.add(entry);
            try{
            	files.addAll(entry.getAffectedFiles());
            } catch (UnsupportedOperationException e) {
            	logger.info(e.getMessage());
            	return null;
            }
        }
        if (entries.isEmpty()) {
            logger.info("Empty change...");
            return null;
        }
        Set<String> authors = new HashSet<String>();
        for (Entry entry : entries) {
            authors.add(entry.getAuthor().getDisplayName());
        }
        MessageBuilder message = new MessageBuilder(r);
        message.append("Started by changes from ");
        message.append(StringUtils.join(authors, ", "));
        message.append(" (");
        message.append(files.size());
        message.append(" file(s) changed)");
        return message.appendOpenLink().toString();
    }

    static String getBuildColor(AbstractBuild r) {
        Result result = r.getResult();
        if (result == Result.SUCCESS) {
            return "green";
        } else if (result == Result.FAILURE) {
            return "red";
        } else {
            return "yellow";
        }
    }

    String getBuildStatusMessage(AbstractBuild r, List<String> culpritsInHipchat) {
        MessageBuilder message = new MessageBuilder(r);
        message.appendStatusMessage();
        message.appendDuration();
        message.appendCulprits(culpritsInHipchat);
        return message.appendOpenLink().toString();
    }

    public static class MessageBuilder {
        private final StringBuffer message;
        private final AbstractBuild build;

        public MessageBuilder(AbstractBuild build) {
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
            return this;
        }

        public MessageBuilder appendOpenLink() {
            String url = Jenkins.getInstance().getRootUrl() + build.getUrl();
            message.append(" (<a href='").append(url).append("'>Open</a>)");
            return this;
        }

        public MessageBuilder appendDuration() {
            message.append(" after ");
            message.append(build.getDurationString());
            return this;
        }

        public MessageBuilder appendCulprits(List<String> culprits) {
            if(culprits != null && culprits.size()>0) {
                message.append(" Committers: ");
                for (String hipchatUsername : culprits) {
                    message.append(hipchatUsername);
                    message.append(" ");
                }
            }
            return this;
        }

        private String getEmailOrFullNameForUser(User user, HipChatService hipChat) {
            Mailer.UserProperty email = user.getProperty(Mailer.UserProperty.class);
            if(email != null && !StringUtils.isEmpty(email.getAddress()
            )){
                return hipChat.getMentionNameForEmail(email.getAddress());
            }else{
                return user.getFullName();
            }
        }

        @Override
        public String toString() {
            return message.toString();
        }
    }
}
