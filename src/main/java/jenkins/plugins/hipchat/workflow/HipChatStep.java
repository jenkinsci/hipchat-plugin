package jenkins.plugins.hipchat.workflow;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.HipChatNotifier;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.impl.HipChatV1Service;
import jenkins.plugins.hipchat.impl.HipChatV2Service;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class HipChatStep extends AbstractStepImpl {

    private static final Logger logger = Logger.getLogger(HipChatStep.class.getName());

    public final String message;

    @DataBoundSetter
    public String color;

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

        public ListBoxModel doFillColorItems(){
            ListBoxModel listBoxModel = new ListBoxModel();
            for(Color color : Color.values()) {
                listBoxModel.add(new ListBoxModel.Option(color.toString(), color.toString(), false));
            }

           return listBoxModel;
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
                throw new AbortException("HipChat message not sent. Messasge property must be supplied.");
            }

            //default to global config values if not set in step, but allow step to override all global settings
            HipChatNotifier.DescriptorImpl hipChatDesc = (HipChatNotifier.DescriptorImpl) Jenkins.getInstance().getDescriptor(HipChatNotifier.class);
            String token = step.token != null ? step.token : hipChatDesc.getToken();
            String room = step.room != null ? step.room : hipChatDesc.getRoom();
            String server = step.server != null ? step.server : hipChatDesc.getServer();
            String sendAs = step.sendAs != null ? step.sendAs : hipChatDesc.getSendAs();
            //default to gray if not set in step
            String color = step.color != null ? step.color : Color.GRAY.toString();
            boolean v2enabled = step.v2enabled != null ? step.v2enabled : hipChatDesc.isV2Enabled();

            //not very elegant, but getHipChatService is private in HipChatNotifier
            HipChatService hipChatService;
            if(v2enabled) {
                hipChatService = new HipChatV2Service(server, token, room);
            } else {
                hipChatService = new HipChatV1Service(server, token, room, sendAs);
            }

            listener.getLogger().println(Messages.WorkflowStepConfig(step.server == null, step.token == null, step.room == null, step.color == null));

            logger.log(Level.WARNING, "HipChat publish settings: api v2 - " + v2enabled + " server - " +
                    server + " token - " + token + " room - " + room);

            hipChatService.publish(step.message, color, step.notify);
            return null;
        }

    }

}
