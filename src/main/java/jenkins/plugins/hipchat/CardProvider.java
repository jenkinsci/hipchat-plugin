package jenkins.plugins.hipchat;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.hipchat.model.notifications.Attribute;
import jenkins.plugins.hipchat.model.notifications.Card;
import jenkins.plugins.hipchat.model.notifications.Icon;
import jenkins.plugins.hipchat.model.notifications.Value;

/**
 * An extension point that can be used to allow full control over HipChat notification cards for individual
 * notifications. The provider itself is globally configured, so any job-specific behavior would be the responsibility
 * of the plugin itself.
 */
public abstract class CardProvider extends AbstractDescribableImpl<CardProvider> implements ExtensionPoint {

    @Override
    public CardProviderDescriptor getDescriptor() {
        return (CardProviderDescriptor) super.getDescriptor();
    }

    /**
     * Returns a card corresponding to the build notification.
     *
     * @param run The build run.
     * @param taskListener The taskListener associated with the current build.
     * @param icon The icon to include in the message.
     * @param message The fully resolved notification message.
     * @return The card that has been constructed for this notification. May be null if no card should be displayed.
     */
    public abstract Card getCard(Run<?, ?> run, TaskListener taskListener, Icon icon, String message);

    /**
     * A simple factory method to easily create attribute for the Card. Attributes are individual information fields
     * that can be displayed on the card. See HipChat API reference for more details.
     *
     * @param label The label for the data.
     * @param value The data value corresponding to the label.
     * @param style The style that determines the color of the background for the value.
     * @param url Used as the href of a link that will be generated (the link's title will be the 'value') on the card.
     * @return The Attribute representation of the provided details.
     */
    protected Attribute attribute(String label, String value, Value.Style style, String url) {
        return new Attribute().withLabel(label).withValue(new Value().withLabel(value).withStyle(style).withUrl(url));
    }
}
