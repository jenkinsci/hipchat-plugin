package jenkins.plugins.hipchat.impl;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.CardProviderDescriptor;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.model.notifications.Card;
import jenkins.plugins.hipchat.model.notifications.Icon;

@Extension
public class NoopCardProvider extends CardProvider {

    @Override
    public Card getCard(Run<?, ?> run, TaskListener taskListener, Icon icon, String message) {
        return null;
    }

    @Override
    public CardProviderDescriptor getDescriptor() {
        return new DescriptorImpl();
    }
    
    @Extension
    public static class DescriptorImpl extends CardProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.NoopCardProvider();
        }
    }
}
