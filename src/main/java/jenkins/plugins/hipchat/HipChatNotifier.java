package jenkins.plugins.hipchat;

import static jenkins.plugins.hipchat.utils.GuiceUtils.*;
import static jenkins.plugins.hipchat.model.NotificationType.*;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import jenkins.plugins.hipchat.impl.HipChatV1Service;
import jenkins.plugins.hipchat.impl.HipChatV2Service;
import jenkins.plugins.hipchat.model.Color;
import jenkins.plugins.hipchat.model.MatrixTriggerMode;
import jenkins.plugins.hipchat.model.NotificationConfig;
import jenkins.plugins.hipchat.model.NotificationType;
import jenkins.plugins.hipchat.utils.BuildUtils;
import jenkins.plugins.hipchat.utils.CredentialUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.AncestorInPath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class HipChatNotifier extends Notifier implements MatrixAggregatable {

    private static final Logger logger = Logger.getLogger(HipChatNotifier.class.getName());

    private transient String token;
    private transient boolean startNotification;
    private transient boolean notifySuccess;
    private transient boolean notifyAborted;
    private transient boolean notifyNotBuilt;
    private transient boolean notifyUnstable;
    private transient boolean notifyFailure;
    private transient boolean notifyBackToNormal;
    private String credentialId;
    private String room;
    private List<NotificationConfig> notifications;
    private MatrixTriggerMode matrixTriggerMode;

    private String startJobMessage;
    private String completeJobMessage;

    @DataBoundConstructor
    public HipChatNotifier(String credentialId, String room, List<NotificationConfig> notifications,
            MatrixTriggerMode matrixTriggerMode, String startJobMessage, String completeJobMessage) {
        this.credentialId = credentialId;
        this.room = room;
        this.notifications = notifications;
        this.matrixTriggerMode = matrixTriggerMode;

        this.startJobMessage = startJobMessage;
        this.completeJobMessage = completeJobMessage;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public void setStartNotification(boolean startNotification) {
        this.startNotification = startNotification;
    }

    public void setNotifySuccess(boolean notifySuccess) {
        this.notifySuccess = notifySuccess;
    }

    public void setNotifyAborted(boolean notifyAborted) {
        this.notifyAborted = notifyAborted;
    }

    public void setNotifyNotBuilt(boolean notifyNotBuilt) {
        this.notifyNotBuilt = notifyNotBuilt;
    }

    public void setNotifyUnstable(boolean notifyUnstable) {
        this.notifyUnstable = notifyUnstable;
    }

    public void setNotifyFailure(boolean notifyFailure) {
        this.notifyFailure = notifyFailure;
    }

    public void setNotifyBackToNormal(boolean notifyBackToNormal) {
        this.notifyBackToNormal = notifyBackToNormal;
    }

    public MatrixTriggerMode getMatrixTriggerMode() {
        return matrixTriggerMode == null ? MatrixTriggerMode.BOTH : matrixTriggerMode;
    }

    public void setMatrixTriggerMode(MatrixTriggerMode matrixTriggerMode) {
        this.matrixTriggerMode = matrixTriggerMode;
    }

    public void setNotifications(List<NotificationConfig> notifications) {
        this.notifications = notifications;
    }

    public List<NotificationConfig> getNotifications() {
        return notifications;
    }
    /* notification message configurations */

    public String getStartJobMessage() {
        return startJobMessage;
    }

    public void setStartJobMessage(String startJobMessage) {
        this.startJobMessage = startJobMessage;
    }

    public String getCompleteJobMessage() {
        return completeJobMessage;
    }

    public void setCompleteJobMessage(String completeJobMessage) {
        this.completeJobMessage = completeJobMessage;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Object readResolve() {
        if (notifications == null) {
            notifications = new ArrayList<NotificationConfig>(7);
            if (startNotification) {
                notifications.add(new NotificationConfig(false, false, STARTED, Color.GREEN, null));
            }
            if (notifySuccess) {
                notifications.add(new NotificationConfig(false, false, SUCCESS, Color.GREEN, null));
            }
            if (notifyAborted) {
                notifications.add(new NotificationConfig(true, false, ABORTED, Color.GRAY, null));
            }
            if (notifyNotBuilt) {
                notifications.add(new NotificationConfig(true, false, NOT_BUILT, Color.GRAY, null));
            }
            if (notifyUnstable) {
                notifications.add(new NotificationConfig(true, false, UNSTABLE, Color.YELLOW, null));
            }
            if (notifyFailure) {
                notifications.add(new NotificationConfig(true, false, FAILURE, Color.RED, null));
            }
            if (notifyBackToNormal) {
                notifications.add(new NotificationConfig(false, false, BACK_TO_NORMAL, Color.GREEN, null));
            }
        }
        return this;
    }

    /**
     * Return the room name defined in the job configuration, or if that's empty return the room name from the global
     * configuration.
     * If the room name is parameterized, this will also try to resolve those parameters.
     *
     * @param build The current build for which we need to get the room.
     * @return The room name tied to the current build.
     */
    public String getResolvedRoom(AbstractBuild<?, ?> build) {
        return Util.replaceMacro(StringUtils.isBlank(room) ? getDescriptor().getRoom() : room,
                build.getBuildVariableResolver());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        //This is here to ensure that the reported build status is actually correct. If we were to return false here,
        //other build plugins could still modify the build result, making the sent out HipChat notification incorrect.
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        logger.fine("Creating build start notification");
        if (!(build instanceof MatrixRun) || getMatrixTriggerMode().forChild) {
            publishNotificationIfEnabled(STARTED, build, listener);
        }

        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (!(build instanceof MatrixRun) || getMatrixTriggerMode().forChild) {
            notifyOnBuildComplete(build, listener);
        }

        return true;
    }

    private void notifyOnBuildComplete(AbstractBuild<?, ?> build, BuildListener listener) {
        logger.fine("Creating build completed notification");
        Result result = build.getResult();
        Result previousResult = get(BuildUtils.class).findPreviousBuildResult(build);

        NotificationType notificationType = NotificationType.fromResults(previousResult, result);
        publishNotificationIfEnabled(notificationType, build, listener);
    }

    private void publishNotificationIfEnabled(NotificationType notificationType, AbstractBuild<?, ?> build,
            BuildListener listener) {
        logger.log(Level.FINE, "Checking if notification {0} is enabled", notificationType);
        NotificationConfig notificationConfig = getNotificationConfig(notificationType);
        if (notificationConfig != null) {
            logger.log(Level.FINE, "Notification config found for notification type {0}: {1}",
                    new Object[]{notificationType, notificationConfig.toString()});
            String messageTemplate = Util.fixEmpty(notificationConfig.getMessageTemplate());
            if (messageTemplate == null) {
                if (notificationType.isStartType()) {
                    messageTemplate = Util.fixEmpty(getStartJobMessage()) == null
                            ? getDescriptor().getStartJobMessageDefault() : getStartJobMessage();
                } else {
                    messageTemplate = Util.fixEmpty(getCompleteJobMessage()) == null
                            ? getDescriptor().getCompleteJobMessageDefault() : getCompleteJobMessage();
                }
            }

            try {
                getHipChatService(build).publish(notificationConfig, notificationType.getMessage(build, messageTemplate));
                listener.getLogger().println(Messages.NotificationSuccessful(getResolvedRoom(build)));
            } catch (NotificationException ne) {
                listener.getLogger().println(Messages.NotificationFailed(ne.getMessage()));
            }
        }
    }

    private NotificationConfig getNotificationConfig(NotificationType notificationType) {
        List<NotificationConfig> configs = Util.fixNull(notifications).isEmpty()
                ? Util.fixNull(getDescriptor().getDefaultNotifications()) : notifications;
        for (NotificationConfig notificationConfig : configs) {
            if (notificationType.equals(notificationConfig.getNotificationType())) {
                return notificationConfig;
            }
        }
        return null;
    }

    private HipChatService getHipChatService(AbstractBuild<?, ?> build) throws NotificationException {
        DescriptorImpl desc = getDescriptor();
        StringCredentials credentials = get(CredentialUtils.class).resolveCredential(build.getParent(),
                Util.fixEmpty(credentialId) != null ? credentialId : desc.getCredentialId(), desc.getServer());
        if (credentials == null) {
            throw new NotificationException(Messages.CredentialMissing(credentialId));
        }
        return getHipChatService(desc.getServer(), Secret.toString(credentials.getSecret()), desc.isV2Enabled(),
                getResolvedRoom(build), desc.getSendAs());
    }

    /**
     * Obtains a {@link HipChatService} implementation corresponding to the provided settings.
     *
     * @param server The URL for the HipChat server.
     * @param token The auth token to use when sending the notification.
     * @param v2Enabled Whether v1 or v2 API should be used.
     * @param room The room to notify.
     * @param sendAs The username to use as the sender when using the v1 API.
     * @return An API version specific {@link HipChatService} instance.
     */
    public static HipChatService getHipChatService(String server, String token, boolean v2Enabled, String room,
            String sendAs) {
        if (v2Enabled) {
            return new HipChatV2Service(server, token, room);
        } else {
            return new HipChatV1Service(server, token, room, sendAs);
        }
    }

    @Override
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {

            @Override
            public boolean startBuild() throws InterruptedException, IOException {
                if (getMatrixTriggerMode().forParent) {
                    publishNotificationIfEnabled(STARTED, build, listener);
                }
                return true;
            }

            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                if (getMatrixTriggerMode().forParent) {
                    notifyOnBuildComplete(build, listener);
                }
                return true;
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private transient String token;
        private String server = "api.hipchat.com";
        private String credentialId;
        private boolean v2Enabled = false;
        private String room;
        private String sendAs = "Jenkins";
        private List<NotificationConfig> defaultNotifications;
        private static int testNotificationCount = 0;

        public DescriptorImpl() {
            load();
            if (Util.fixEmpty(token) != null) {
                try {
                    get(CredentialUtils.class).migrateGlobalCredential(this);
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Unable to migrate globally stored auth token to a credential", ioe);
                }
            }
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public String getCredentialId() {
            return credentialId;
        }

        public void setCredentialId(String credentialId) {
            this.credentialId = credentialId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public boolean isV2Enabled() {
            return v2Enabled;
        }

        public void setV2Enabled(boolean v2Enabled) {
            this.v2Enabled = v2Enabled;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        public String getSendAs() {
            return sendAs;
        }

        public void setSendAs(String sendAs) {
            this.sendAs = sendAs;
        }

        public List<NotificationConfig> getDefaultNotifications() {
            return defaultNotifications;
        }

        public void setDefaultNotifications(List<NotificationConfig> defaultNotifications) {
            this.defaultNotifications = defaultNotifications;
        }

        /* Default notification messages for UI */
        public String getStartJobMessageDefault() {
            return Messages.JobStarted();
        }

        public String getCompleteJobMessageDefault() {
            return Messages.JobCompleted();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public boolean isMatrixProject(Object project) {
            return project instanceof MatrixProject;
        }

        @Override
        public boolean configure(StaplerRequest request, JSONObject formData) throws FormException {
            request.bindJSON(this, formData);

            save();
            return super.configure(request, formData);
        }

        public FormValidation doCheckSendAs(@QueryParameter boolean v2Enabled, @QueryParameter String sendAs) {
            sendAs = Util.fixEmpty(sendAs);
            if (!v2Enabled) {
                if (sendAs == null || sendAs.length() > 15) {
                    return FormValidation.error(Messages.InvalidSendAs());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doSendTestNotification(@AncestorInPath AbstractProject<?, ?> context,
                @QueryParameter String server, @QueryParameter String credentialId, @QueryParameter boolean v2Enabled,
                @QueryParameter String room, @QueryParameter String sendAs) {
            StringCredentials credentials = get(CredentialUtils.class).resolveCredential(context, credentialId, server);
            if (credentials == null) {
                return FormValidation.error(Messages.CredentialMissing(credentialId));
            }
            HipChatService service = getHipChatService(server, Secret.toString(credentials.getSecret()),
                    v2Enabled, room, sendAs);
            try {
                service.publish(Messages.TestNotification(++testNotificationCount), "yellow", true);
                return FormValidation.ok(Messages.TestNotificationSent());
            } catch (NotificationException ne) {
                return FormValidation.error(Messages.TestNotificationFailed(ne.getMessage()));
            }
        }

        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item context, @QueryParameter String server) {
            return get(CredentialUtils.class).getAvailableCredentials(context, credentialId,
                    Util.fixEmpty(server) == null ? this.server : server);
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

    /**
     * The settings defined here have been moved to the {@link HipChatNotifier} configuration (shows up under the Post
     * Build task view).
     *
     * @deprecated The plugin configuration should be stored in {@link HipChatNotifier}. This class only exists, so
     * configurations can be migrated for the build jobs.
     */
    @Deprecated
    public static class HipChatJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {
        private final String room;
        private final boolean startNotification;
        private final boolean notifySuccess;
        private final boolean notifyAborted;
        private final boolean notifyNotBuilt;
        private final boolean notifyUnstable;
        private final boolean notifyFailure;
        private final boolean notifyBackToNormal;


        @DataBoundConstructor
        public HipChatJobProperty(String room,
                                  boolean startNotification,
                                  boolean notifyAborted,
                                  boolean notifyFailure,
                                  boolean notifyNotBuilt,
                                  boolean notifySuccess,
                                  boolean notifyUnstable,
                                  boolean notifyBackToNormal) {
            this.room = room;
            this.startNotification = startNotification;
            this.notifyAborted = notifyAborted;
            this.notifyFailure = notifyFailure;
            this.notifyNotBuilt = notifyNotBuilt;
            this.notifySuccess = notifySuccess;
            this.notifyUnstable = notifyUnstable;
            this.notifyBackToNormal = notifyBackToNormal;
        }

        @Exported
        public String getRoom() {
            return room;
        }

        @Exported
        public boolean getStartNotification() {
            return startNotification;
        }

        @Exported
        public boolean getNotifySuccess() {
            return notifySuccess;
        }

        @Exported
        public boolean getNotifyAborted() {
            return notifyAborted;
        }

        @Exported
        public boolean getNotifyFailure() {
            return notifyFailure;
        }

        @Exported
        public boolean getNotifyNotBuilt() {
            return notifyNotBuilt;
        }

        @Exported
        public boolean getNotifyUnstable() {
            return notifyUnstable;
        }

        @Exported
        public boolean getNotifyBackToNormal() {
            return notifyBackToNormal;
        }

        @Extension
        public static final class DescriptorImpl extends JobPropertyDescriptor {
            public String getDisplayName() {
                return "HipChat Notifications";
            }

            @Override
            public boolean isApplicable(Class<? extends Job> jobType) {
                return true;
            }

            @Override
            public HipChatJobProperty newInstance(StaplerRequest sr, JSONObject formData) throws FormException {
                if (sr == null) {
                    throw new IllegalArgumentException("staplerRequest must not be null");
                }
                return new HipChatJobProperty(sr.getParameter("hipChatProjectRoom"),
                        sr.getParameter("hipChatStartNotification") != null,
                        sr.getParameter("hipChatNotifyAborted") != null,
                        sr.getParameter("hipChatNotifyFailure") != null,
                        sr.getParameter("hipChatNotifyNotBuilt") != null,
                        sr.getParameter("hipChatNotifySuccess") != null,
                        sr.getParameter("hipChatNotifyUnstable") != null,
                        sr.getParameter("hipChatNotifyBackToNormal") != null);
            }
        }
    }
}
