package jenkins.plugins.hipchat.model;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.model.notifications.Notification.Color;
import org.kohsuke.stapler.DataBoundConstructor;

public class NotificationConfig implements Describable<NotificationConfig> {

    private final boolean notifyEnabled;
    private final boolean textFormat;
    private final NotificationType notificationType;
    private final Color color;
    private final String icon;
    private final String messageTemplate;

    @DataBoundConstructor
    public NotificationConfig(boolean notifyEnabled, boolean textFormat, NotificationType notificationType, Color color,
            String icon, String messageTemplate) {
        this.notifyEnabled = notifyEnabled;
        this.textFormat = textFormat;
        this.notificationType = notificationType;
        this.color = color;
        this.icon = icon;
        this.messageTemplate = messageTemplate;
    }

    public boolean isNotifyEnabled() {
        return notifyEnabled;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public boolean isTextFormat() {
        return textFormat;
    }

    public Color getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Returns a copy of the notification config that will contain the same settings, but the message template will be
     * overridden with the freshly provided one.
     *
     * @param messageTemplate The new message template to use.
     * @return A new {@link NotificationConfig} instance that has its message template updated.
     */
    public NotificationConfig overrideMessageTemplate(String messageTemplate) {
        return new NotificationConfig(notifyEnabled, textFormat, notificationType, color, icon, messageTemplate);
    }

    @Override
    public Descriptor<NotificationConfig> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Override
    public String toString() {
        return "Notification{" + "notifyEnabled=" + notifyEnabled + ", notificationType=" + notificationType
                + ", color=" + color + ", icon=" + icon + ", messageTemplate=" + messageTemplate + '}';
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<NotificationConfig> {

        @Override
        public String getDisplayName() {
            return "HipChat Notification";
        }
    }
}
