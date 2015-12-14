package jenkins.plugins.hipchat.workflow;

import hudson.model.Result;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.model.Color;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class HipChatSendStepTest {
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();


    @Test
    public void configRoundTrip() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                HipChatSendStep step1 = new HipChatSendStep("message");
                step1.color = Color.GREEN;
                step1.room = "room";
                step1.v2enabled = true;
                step1.notify = false;

                HipChatSendStep step2 = new StepConfigTester(story.j).configRoundTrip(step1);
                story.j.assertEqualDataBoundBeans(step1, step2);
            }
        });
    }

    @Test
    public void test_publish_message() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchatSend 'message';", true));
                WorkflowRun run = story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));
                //everything should come from global configuration
                story.j.assertLogContains(Messages.WorkflowStepConfig(true, true, true, true), run);
            }
        });
    }

    @Test
    public void test_empty_message() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchatSend(message: '');", true));
                WorkflowRun run = story.j.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
                //should result in an error in log
                story.j.assertLogContains(Messages.MessageRequiredError(), run);
            }
        });
    }

    @Test
    public void test_empty_message_fail_on_error() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchatSend(message: '', failOnError: true);", true));
                WorkflowRun run = story.j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
                //should result in an error in log
                story.j.assertLogContains(Messages.MessageRequiredError(), run);
            }
        });
    }

    @Test
    public void test_global_config_override() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchatSend(message: 'message', server: 'server', token: 'token', room: 'room', color: 'GREEN', v2enabled: true);", true));
                WorkflowRun run = story.j.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
                //everything should come from step configuration
                story.j.assertLogContains(Messages.WorkflowStepConfig(false, false, false, false), run);
            }
        });
    }

    @Test
    public void test_fail_on_error() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchatSend(message: 'message', server: 'server', token: 'token', room: 'room', color: 'GREEN', v2enabled: true, failOnError: true);", true));
                WorkflowRun run = story.j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
                //everything should come from step configuration
                story.j.assertLogContains(Messages.NotificationFailed(""), run);
            }
        });
    }

}
