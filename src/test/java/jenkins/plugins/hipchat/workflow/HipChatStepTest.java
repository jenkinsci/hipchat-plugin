package jenkins.plugins.hipchat.workflow;

import hudson.model.Result;
import jenkins.plugins.hipchat.Messages;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class HipChatStepTest {
    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();


    @Test
    public void configRoundTrip() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                HipChatStep step1 = new HipChatStep("message");
                step1.color = "green";
                step1.room = "room";
                step1.v2enabled = true;
                step1.notify = false;

                HipChatStep step2 = new StepConfigTester(story.j).configRoundTrip(step1);
                story.j.assertEqualDataBoundBeans(step1, step2);
            }
        });
    }

    @Test
    public void test_messasge() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchat 'message';", true));
                WorkflowRun run = story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));
                //everything should come from global configuration
                story.j.assertLogContains(Messages.WorkflowStepConfig(true, true, true, true), run);
            }
        });
    }

    @Test
    public void test_missing_message() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchat(message: '');", true));
                WorkflowRun run = story.j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
                //everything should come from global configuration
                story.j.assertLogContains("HipChat message not sent. Messasge property must be supplied.", run);
            }
        });
    }

    @Test
    public void test_glob_config_ovveride() throws Exception {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "workflow");
                //just define message
                job.setDefinition(new CpsFlowDefinition("hipchat(message: 'message', server: 'server', token: 'token', room: 'room', color: 'green', v2enabled: true);", true));
                WorkflowRun run = story.j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
                //everything should come from global configuration
                story.j.assertLogContains(Messages.WorkflowStepConfig(false, false, false, false), run);
            }
        });
    }

}
