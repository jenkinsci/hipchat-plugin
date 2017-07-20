package jenkins.plugins.hipchat.upgrade;

import static jenkins.plugins.hipchat.utils.GuiceUtils.get;

import hudson.BulkChange;
import hudson.Extension;
import hudson.Plugin;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.listeners.ItemListener;
import hudson.util.VersionNumber;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.HipChatNotifier;
import jenkins.plugins.hipchat.HipChatNotifier.DescriptorImpl;
import jenkins.plugins.hipchat.HipChatNotifier.HipChatJobProperty;
import jenkins.plugins.hipchat.utils.CredentialUtils;

@Extension
public class ConfigurationMigrator extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationMigrator.class.getName());

    @Override
    public void onLoaded() {
        Jenkins jenkins = Jenkins.getInstance();
        HipChatNotifier.DescriptorImpl descriptor = jenkins.getDescriptorByType(DescriptorImpl.class);
        Plugin plugin = jenkins.getPlugin("hipchat");
        if (plugin == null) {
            return;
        }
        VersionNumber pluginVersion = plugin.getWrapper().getVersionNumber();
        if (pluginVersion.isOlderThan(new VersionNumber(descriptor.getConfigVersion()))) {
            return;
        }

        for (AbstractProject<?, ?> item : jenkins.getAllItems(AbstractProject.class)) {
            HipChatNotifier notifier = item.getPublishersList().get(HipChatNotifier.class);
            HipChatJobProperty property = item.getProperty(HipChatJobProperty.class);
            BulkChange bc = new BulkChange(item);
            try {
                if (property != null) {
                    if (notifier != null) {
                        notifier.setRoom(property.getRoom());
                        notifier.setStartNotification(property.getStartNotification());
                        notifier.setNotifyAborted(property.getNotifyAborted());
                        notifier.setNotifyBackToNormal(property.getNotifyBackToNormal());
                        notifier.setNotifyFailure(property.getNotifyFailure());
                        notifier.setNotifyNotBuilt(property.getNotifyNotBuilt());
                        notifier.setNotifySuccess(property.getNotifySuccess());
                        notifier.setNotifyUnstable(property.getNotifyUnstable());
                        notifier.setNotifications(null);
                        notifier.readResolve();
                    }
                    try {
                        item.removeProperty(HipChatJobProperty.class);
                        LOGGER.log(Level.INFO, "Successfully migrated project configuration for build job: {0}",
                                item.getFullDisplayName());
                    } catch (IOException ioe) {
                        LOGGER.log(Level.WARNING, "An error occurred while trying to update job configuration for "
                                + item.getName(), ioe);
                    }
                }
                if (notifier != null && Util.fixEmpty(notifier.getToken()) != null) {
                    LOGGER.log(Level.FINER, "Attempting to migrate credentials for job: {0}",
                            item.getFullDisplayName());
                    get(CredentialUtils.class).migrateJobCredential(descriptor, item, notifier);
                    LOGGER.log(Level.FINER, "Successfully migrated credential for job: {0}", item.getFullDisplayName());
                    item.save();
                }
                bc.commit();
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "Unable to save configuration for job: " + item.getFullName(), ioe);
            } finally {
                bc.abort();
            }
        }
        descriptor.setConfigVersion(pluginVersion.toString());
        descriptor.save();
    }
}
