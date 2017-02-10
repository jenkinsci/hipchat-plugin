package jenkins.plugins.hipchat.impl;

import static jenkins.plugins.hipchat.model.Constants.*;
import static jenkins.plugins.hipchat.model.notifications.Value.Style.*;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.CardProviderDescriptor;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.model.Constants;
import jenkins.plugins.hipchat.model.notifications.Activity;
import jenkins.plugins.hipchat.model.notifications.Attribute;
import jenkins.plugins.hipchat.model.notifications.Card;
import jenkins.plugins.hipchat.model.notifications.Card.Style;
import jenkins.plugins.hipchat.model.notifications.Icon;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class DefaultCardProvider extends CardProvider {

    private static final Logger LOGGER = Logger.getLogger(DefaultCardProvider.class.getName());

    @Override
    public Card getCard(Run<?, ?> run, TaskListener taskListener, String message) {
        Icon icon = new Icon().withUrl("https://bit.ly/2ctIstd");
        try {
            return new Card()
                    .withStyle(Style.APPLICATION)
                    .withUrl(TokenMacro.expandAll(run, null, taskListener, Constants.BUILD_URL_MACRO))
                    .withFormat(Card.Format.MEDIUM)
                    .withId(UUID.randomUUID().toString())
                    .withTitle(TokenMacro.expandAll(run, null, taskListener, Messages.CardTitle(), false, null))
                    .withIcon(icon)
                    .withAttributes(getAttributes(run, taskListener))
                    .withActivity(new Activity()
                            .withHtml(message)
                            .withIcon(icon));
        } catch (MacroEvaluationException | IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to resolve token macros", ex);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    private List<Attribute> getAttributes(Run<?, ?> run, TaskListener taskListener)
            throws MacroEvaluationException, IOException, InterruptedException {
        List<Attribute> ret = new ArrayList<>();
        String count = TokenMacro.expand(run, null, taskListener, SUCCESS_TEST_COUNT_MACRO);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsSuccessful(), count,
                    "0".equals(count) ? LOZENGE_ERROR : LOZENGE_SUCCESS, null));
        }
        count = TokenMacro.expand(run, null, taskListener, FAILED_TEST_COUNT_MACRO);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsFailed(), count,
                    "0".equals(count) ? LOZENGE_SUCCESS : LOZENGE_ERROR, null));
        }
        count = TokenMacro.expand(run, null, taskListener, SKIPPED_TEST_COUNT_MACRO);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsSkipped(), count,
                    "0".equals(count) ? LOZENGE_SUCCESS : LOZENGE_CURRENT, null));
        }
        if (!ret.isEmpty()) {
            ret.add(attribute(Messages.TestReport(), Messages.Here(), null,
                    TokenMacro.expand(run, null, taskListener, TEST_REPORT_URL_MACRO)));
        }
        return ret.isEmpty() ? null : ret;
    }

    @Override
    public CardProviderDescriptor getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension
    public static class DescriptorImpl extends CardProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.DefaultCardProvider();
        }
    }
}
