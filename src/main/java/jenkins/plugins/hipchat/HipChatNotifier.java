package jenkins.plugins.hipchat;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class HipChatNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(HipChatNotifier.class.getName());

    private Boolean v2API;
    private String server;
    private String authToken;
    private String buildServerUrl;
    private String room;
    private String sendAs;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getServer() {
        return this.server;
    }

    public String getRoom() {
        return this.room;
    }

    public String getAuthToken() {
        return this.authToken;
    }

    public String getBuildServerUrl() {
        return this.buildServerUrl;
    }

    public String getSendAs() {
        return this.sendAs;
    }

    public Boolean isV2API() {
        return v2API;
    }

    @DataBoundConstructor
    public HipChatNotifier(final String server, final String authToken, final String room, String buildServerUrl, final String sendAs, final boolean v2API) {
        super();
        this.server = server;
        this.authToken = authToken;
        this.buildServerUrl = buildServerUrl;
        this.room = room;
        this.sendAs = sendAs;
        this.v2API = v2API;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public HipChatService newHipChatService(final String room) {
        return new StandardHipChatService(getServer(), getAuthToken(), room == null ? getRoom() : room, StringUtils.isBlank(getSendAs()) ? "Build Server" : getSendAs(), isV2API());
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String server;
        private String token;
        private String room;
        private String buildServerUrl;
        private String sendAs;
        private Boolean v2API;

        public DescriptorImpl() {
            load();
        }

        public String getServer() {
            return server;
        }

        public String getToken() {
            return token;
        }

        public String getRoom() {
            return room;
        }

        public String getBuildServerUrl() {
            return buildServerUrl;
        }

        public String getSendAs() {
            return sendAs;
        }

        public Boolean getV2API() {
            return v2API;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public HipChatNotifier newInstance(StaplerRequest sr) {
            if (server == null) server = sr.getParameter("hipChatServer");
            if (token == null) token = sr.getParameter("hipChatToken");
            if (buildServerUrl == null) buildServerUrl = sr.getParameter("hipChatBuildServerUrl");
            if (room == null) room = sr.getParameter("hipChatRoom");
            if (sendAs == null) sendAs = sr.getParameter("hipChatSendAs");
            if (v2API == null) v2API = Boolean.valueOf(sr.getParameter("hipChatAPIV2")).booleanValue();
            return new HipChatNotifier(server, token, room, buildServerUrl, sendAs, v2API);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            server = sr.getParameter("hipChatServer");
            token = sr.getParameter("hipChatToken");
            room = sr.getParameter("hipChatRoom");
            buildServerUrl = sr.getParameter("hipChatBuildServerUrl");
            sendAs = sr.getParameter("hipChatSendAs");
            v2API = Boolean.valueOf(sr.getParameter("hipChatAPIV2"));
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            try {
                new HipChatNotifier(server, token, room, buildServerUrl, sendAs, v2API);
            } catch (Exception e) {
                throw new FormException("Failed to initialize notifier - check your global notifier configuration settings", e, "");
            }
            save();
            return super.configure(sr, formData);
        }

        @Override
        public String getDisplayName() {
            return "HipChat Notifications";
        }
    }

    public static class HipChatJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {
        private String room;
        private boolean startNotification;
        private boolean notifySuccess;
        private boolean notifyAborted;
        private boolean notifyNotBuilt;
        private boolean notifyUnstable;
        private boolean notifyFailure;
        private boolean notifyBackToNormal;


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

        @Override
        public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            if (startNotification) {
                Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
                for (Publisher publisher : map.values()) {
                    if (publisher instanceof HipChatNotifier) {
                        logger.info("Invoking Started...");
                        new ActiveNotifier((HipChatNotifier) publisher).started(build);
                    }
                }
            }
            return super.prebuild(build, listener);
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
            public HipChatJobProperty newInstance(StaplerRequest sr, JSONObject formData) throws hudson.model.Descriptor.FormException {
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
