package jenkins.plugins.hipchat.impl;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.Run;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.CardProviderDescriptor;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.model.notifications.Card;

@Extension
public class NoopCardProvider extends CardProvider {

    @Override
    public Card getCard(Run<?, ?> run, BuildListener buildListener, String message) {
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
