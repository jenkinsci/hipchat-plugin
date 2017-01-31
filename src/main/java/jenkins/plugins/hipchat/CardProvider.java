package jenkins.plugins.hipchat;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import java.util.Map;
import jenkins.plugins.hipchat.model.notifications.Attribute;
import jenkins.plugins.hipchat.model.notifications.Card;
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
     * @param params Parameters that were already collected as part of the notification message generation.
     * Implementations are advised to reuse already collected build information from here, rather than recalculating
     * them again based on the run.
     * @return The card that has been constructed for this notification. May be null if no card should be displayed.
     */
    public abstract Card getCard(Run<?, ?> run, Map<String, String> params);

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
