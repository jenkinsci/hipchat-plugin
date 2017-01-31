package jenkins.plugins.hipchat.impl;

import static jenkins.plugins.hipchat.model.Constants.*;
import static jenkins.plugins.hipchat.model.notifications.Value.Style.*;

import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.util.VariableResolver;
import hudson.util.VariableResolver.ByMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.CardProviderDescriptor;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.model.notifications.Activity;
import jenkins.plugins.hipchat.model.notifications.Attribute;
import jenkins.plugins.hipchat.model.notifications.Card;
import jenkins.plugins.hipchat.model.notifications.Card.Style;
import jenkins.plugins.hipchat.model.notifications.Icon;
import org.apache.commons.lang.StringUtils;

@Extension
public class DefaultCardProvider extends CardProvider {

    @Override
    public Card getCard(Run<?, ?> run, Map<String, String> params) {
        Icon icon = new Icon().withUrl("http://bit.ly/2ctIstd");
        ByMap<String> resolver = new VariableResolver.ByMap<>(params);
        String messageTemplate = params.get(HIPCHAT_MESSAGE_TEMPLATE);
        return new Card()
                .withStyle(Style.APPLICATION)
                .withUrl(params.get(URL))
                .withFormat(Card.Format.MEDIUM)
                .withId(UUID.randomUUID().toString())
                .withTitle(Util.replaceMacro(Messages.CardTitle(), resolver))
                .withIcon(icon)
                .withAttributes(getAttributes(run, params))
                .withActivity(new Activity()
                        .withHtml(Util.replaceMacro(messageTemplate, resolver))
                        .withIcon(icon));
    }

    private List<Attribute> getAttributes(Run<?, ?> run, Map<String, String> params) {
        List<Attribute> ret = new ArrayList<>();
        String count = params.get(SUCCESS_TEST_COUNT);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsSuccessful(), count,
                    "0".equals(count) ? LOZENGE_ERROR : LOZENGE_SUCCESS, null));
        }
        count = params.get(FAILED_TEST_COUNT);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsFailed(), count,
                    "0".equals(count) ? LOZENGE_SUCCESS : LOZENGE_ERROR, null));
        }
        count = params.get(SKIPPED_TEST_COUNT);
        if (StringUtils.isNotEmpty(count)) {
            ret.add(attribute(Messages.TestsSkipped(), count,
                    "0".equals(count) ? LOZENGE_SUCCESS : LOZENGE_CURRENT, null));
        }
        if (!ret.isEmpty()) {
            ret.add(attribute(Messages.TestReport(), Messages.Here(), null,
                    params.get(URL) + params.get(TEST_REPORT_PATH)));
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
