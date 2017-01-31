package jenkins.plugins.hipchat.model;

import static jenkins.plugins.hipchat.model.Constants.*;
import static jenkins.plugins.hipchat.utils.GuiceUtils.*;

import com.google.common.annotations.VisibleForTesting;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.util.VariableResolver;
import hudson.util.VariableResolver.ByMap;
import java.util.Map;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.HipChatNotifier.DescriptorImpl;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.impl.NoopCardProvider;
import jenkins.plugins.hipchat.model.notifications.Notification;
import jenkins.plugins.hipchat.model.notifications.Notification.MessageFormat;
import jenkins.plugins.hipchat.utils.BuildUtils;

public enum NotificationType {

    STARTED(true) {

                @Override
                public String getStatus() {
                    return Messages.Started();
                }
            },
    ABORTED {

                @Override
                public String getStatus() {
                    return Messages.Aborted();
                }
            },
    SUCCESS {

                @Override
                public String getStatus() {
                    return Messages.Success();
                }
            },
    FAILURE {

                @Override
                public String getStatus() {
                    return Messages.Failure();
                }
            },
    NOT_BUILT {

                @Override
                public String getStatus() {
                    return Messages.NotBuilt();
                }
            },
    BACK_TO_NORMAL {

                @Override
                public String getStatus() {
                    return Messages.BackToNormal();
                }
            },
    UNSTABLE {

                @Override
                public String getStatus() {
                    return Messages.Unstable();
                }
            };

    private final boolean startType;

    private NotificationType() {
        this(false);
    }

    private NotificationType(boolean startType) {
        this.startType = startType;
    }

    public abstract String getStatus();

    public boolean isStartType() {
        return startType;
    }

    public final Notification getNotification(NotificationConfig config, AbstractBuild<?, ?> build) {
        return getNotification(config, build, get(BuildUtils.class), Jenkins.getInstance());
    }

    @VisibleForTesting
    Notification getNotification(NotificationConfig config, AbstractBuild<?, ?> build,
            BuildUtils buildUtils, Jenkins jenkins) {
        Map<String, String> params = buildUtils.collectParametersFor(jenkins, build);
        params.put(STATUS, getStatus());
        params.put(PRINT_FULL_ENV, params.toString());
        params.put(HIPCHAT_MESSAGE_TEMPLATE, config.getMessageTemplate());
        ByMap<String> resolver = new VariableResolver.ByMap<>(params);

        CardProvider cardProvider = ExtensionList.lookup(CardProvider.class)
                .getDynamic(jenkins.getDescriptorByType(DescriptorImpl.class).getCardProvider());
        if (cardProvider == null) {
            cardProvider = new NoopCardProvider();
        }

        return new Notification()
                .withColor(config.getColor())
                .withMessageFormat(config.isTextFormat() ? MessageFormat.TEXT : MessageFormat.HTML)
                .withNotify(config.isNotifyEnabled())
                .withMessage(Util.replaceMacro(config.getMessageTemplate(), resolver))
                .withCard(cardProvider.getCard(build, params));
    }

    public static final NotificationType fromResults(Result previousResult, Result result) {
        if (result == Result.ABORTED) {
            return ABORTED;
        } else if (result == Result.FAILURE) {
            return FAILURE;
        } else if (result == Result.NOT_BUILT) {
            return NOT_BUILT;
        } else if (result == Result.UNSTABLE) {
            return UNSTABLE;
        } else if (result == Result.SUCCESS) {
            if (previousResult != Result.SUCCESS) {
                return BACK_TO_NORMAL;
            } else {
                return SUCCESS;
            }
        }

        throw new IllegalStateException("Unable to determine notification type");
    }
}
