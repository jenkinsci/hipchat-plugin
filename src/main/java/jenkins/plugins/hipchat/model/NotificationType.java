package jenkins.plugins.hipchat.model;

import static hudson.Util.replaceMacro;
import static jenkins.plugins.hipchat.utils.GuiceUtils.*;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.util.VariableResolver;
import java.util.Map;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.Messages;
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

    public final String getMessage(AbstractBuild<?, ?> build, String messageTemplate) {
        return getMessage(get(BuildUtils.class), Jenkins.getInstance(), build, messageTemplate);
    }

    @VisibleForTesting
    String getMessage(BuildUtils buildUtils, Jenkins jenkins, AbstractBuild<?, ?> build, String messageTemplate) {
        Map<String, String> messageVariables = buildUtils.collectParametersFor(jenkins, build);
        messageVariables.put("STATUS", getStatus());
        messageVariables.put("PRINT_FULL_ENV", messageVariables.toString());

        return replaceMacro(messageTemplate, new VariableResolver.ByMap<String>(messageVariables));
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
