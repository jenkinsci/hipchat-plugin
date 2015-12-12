package jenkins.plugins.hipchat.workflow;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.HipChatNotifier;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import jenkins.plugins.hipchat.model.Color;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.logging.Logger;

public class HipChatStep extends AbstractStepImpl {

    private static final Logger logger = Logger.getLogger(HipChatStep.class.getName());

    public final String message;

    @DataBoundSetter
    public Color color;

    @DataBoundSetter
    public String token;

    @DataBoundSetter
    public String room;

    @DataBoundSetter
    public String server;

    @DataBoundSetter
    public boolean notify;

    @DataBoundSetter
    public Boolean v2enabled;

    @DataBoundSetter
    public String sendAs;

    @DataBoundSetter
    public boolean failOnError;

    @DataBoundConstructor
    public HipChatStep(@Nonnull String message) {
        this.message = message;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(HipChatStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "hipchat";
        }

        @Override
        public String getDisplayName() {
            return "Publish HipChat Message";
        }

    }

    public static class HipChatStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient HipChatStep step;
        @StepContextParameter
        transient TaskListener listener;


        @Override
        protected Void run() throws Exception {

            if (StringUtils.isBlank(step.message)) {
                throw new AbortException(Messages.MessageRequiredError());
            }

            //default to global config values if not set in step, but allow step to override all global settings
            HipChatNotifier.DescriptorImpl hipChatDesc = Jenkins.getInstance().getDescriptorByType(HipChatNotifier.DescriptorImpl.class);
            String token = step.token != null ? step.token : hipChatDesc.getToken();
            String room = step.room != null ? step.room : hipChatDesc.getRoom();
            String server = step.server != null ? step.server : hipChatDesc.getServer();
            String sendAs = step.sendAs != null ? step.sendAs : hipChatDesc.getSendAs();
            //default to gray if not set in step
            Color color = step.color != null ? step.color : Color.GRAY;
            boolean v2enabled = step.v2enabled != null ? step.v2enabled : hipChatDesc.isV2Enabled();

            //only way to use this static method from HipChatNotifier and keep HipChatStep in the workflow package is to make it public
            HipChatService hipChatService = HipChatNotifier.getHipChatService(server, token, v2enabled, room, sendAs);

            //placing in console log to simplify testing of retrieving values from global config or from step field
            listener.getLogger().println(Messages.WorkflowStepConfig(step.server == null, step.token == null, step.room == null, step.color == null));

            logger.finer("HipChat publish settings: api v2 - " + v2enabled + " server - " +
                    server + " token - " + token + " room - " + room);

            //attempt to publish message, log NotificationException, will allow run to continue
            try {
                hipChatService.publish(step.message, color.toString(), step.notify);
            } catch (NotificationException ne) {
                //allow entire run to fail based on failOnError field
                if (step.failOnError) {
                    throw new AbortException(Messages.NotificationFailed(ne.getMessage()));
                } else {
                    listener.error(Messages.NotificationFailed(ne.getMessage()));
                }
            }

            return null;
        }

    }

}
