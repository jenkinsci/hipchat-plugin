package jenkins.plugins.hipchat.model;

import static jenkins.plugins.hipchat.model.Constants.*;
import static jenkins.plugins.hipchat.utils.GuiceUtils.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.io.IOException;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.CardProvider;
import jenkins.plugins.hipchat.HipChatNotifier.DescriptorImpl;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import jenkins.plugins.hipchat.impl.NoopCardProvider;
import jenkins.plugins.hipchat.model.notifications.Notification;
import jenkins.plugins.hipchat.model.notifications.Notification.MessageFormat;
import jenkins.plugins.hipchat.utils.BuildUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

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

    public final Notification getNotification(NotificationConfig config, AbstractBuild<?, ?> build,
            BuildListener buildListener) throws NotificationException {
        return getNotification(config, build, buildListener, get(BuildUtils.class), Jenkins.getInstance());
    }

    @VisibleForTesting
    Notification getNotification(NotificationConfig config, AbstractBuild<?, ?> build, BuildListener buildListener,
            BuildUtils buildUtils, Jenkins jenkins) throws NotificationException {
        String messageTemplate = Util.replaceMacro(config.getMessageTemplate(), ImmutableMap.of(STATUS, getStatus()));
        CardProvider cardProvider = ExtensionList.lookup(CardProvider.class)
                .getDynamic(jenkins.getDescriptorByType(DescriptorImpl.class).getCardProvider());
        if (cardProvider == null) {
            cardProvider = new NoopCardProvider();
        }

        try {
            String message = TokenMacro.expandAll(build, buildListener, messageTemplate, false, null);
            return new Notification()
                    .withColor(config.getColor())
                    .withMessageFormat(config.isTextFormat() ? MessageFormat.TEXT : MessageFormat.HTML)
                    .withNotify(config.isNotifyEnabled())
                    .withMessage(message)
                    .withCard(cardProvider.getCard(build, buildListener, config.getIconObject(), message));
        } catch (MacroEvaluationException | IOException ex) {
            buildListener.getLogger().println(Messages.MacroEvaluationFailed(ex.toString()));
            throw new NotificationException(Messages.MacroEvaluationFailed(ex.getMessage()), ex);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new NotificationException(Messages.MacroEvaluationFailed(ie.getMessage()), ie);
        }
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
            if (previousResult != null && previousResult != Result.SUCCESS) {
                return BACK_TO_NORMAL;
            } else {
                return SUCCESS;
            }
        }

        throw new IllegalStateException("Unable to determine notification type");
    }
}
