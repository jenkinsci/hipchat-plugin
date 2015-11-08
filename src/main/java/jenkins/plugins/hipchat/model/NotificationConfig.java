package jenkins.plugins.hipchat.model;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class NotificationConfig implements Describable<NotificationConfig> {

    private final boolean notifyEnabled;
    private final NotificationType notificationType;
    private final Color color;
    private final String messageTemplate;

    @DataBoundConstructor
    public NotificationConfig(boolean notifyEnabled, NotificationType notificationType, Color color,
            String messageTemplate) {
        this.notifyEnabled = notifyEnabled;
        this.notificationType = notificationType;
        this.color = color;
        this.messageTemplate = messageTemplate;
    }

    public boolean isNotifyEnabled() {
        return notifyEnabled;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public Color getColor() {
        return color;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    @Override
    public Descriptor<NotificationConfig> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Override
    public String toString() {
        return "Notification{" + "notifyEnabled=" + notifyEnabled + ", notificationType=" + notificationType
                + ", color=" + color + ", messageTemplate=" + messageTemplate + '}';
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<NotificationConfig> {

        @Override
        public String getDisplayName() {
            return "HipChat Notification";
        }
    }
}
