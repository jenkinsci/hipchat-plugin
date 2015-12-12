package jenkins.plugins.hipchat.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.CauseAction;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jenkins.model.Jenkins;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildUtilsTest {

    @Mock
    private Jenkins jenkins;
    @Mock
    private Run<?, ?> run;
    @Mock
    private AbstractBuild<?, ?> build;
    @Mock
    private Run firstRun;
    @Mock
    private Run secondRun;
    @Mock
    private Run thirdRun;
    @Mock
    private AbstractProject mockProject;
    @Mock
    private ItemGroup mockItemGroup;
    @Mock
    private AbstractTestResultAction mockTestResults;
    private final BuildUtils buildUtils = new BuildUtils();

    @Test
    public void shouldFindPreviousResultAcrossMultipleIterations() {
        given(run.getPreviousBuild()).willReturn(thirdRun);
        given(thirdRun.getResult()).willReturn(Result.ABORTED);
        given(thirdRun.getPreviousBuild()).willReturn(secondRun);
        given(secondRun.getResult()).willReturn(Result.NOT_BUILT);
        given(secondRun.getPreviousBuild()).willReturn(firstRun);
        given(firstRun.getResult()).willReturn(Result.SUCCESS);

        Result result = buildUtils.findPreviousBuildResult(run);

        assertThat(result).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void shouldReturnNullIfPreviousBuildIsNull() {
        given(run.getPreviousBuild()).willReturn(null);

        Result result = buildUtils.findPreviousBuildResult(run);

        assertThat(result).isNull();
    }

    @Test
    public void shouldReturnNullIfPreviousBuildIsStillRunning() {
        given(run.getPreviousBuild()).willReturn(firstRun);
        given(firstRun.getResult()).willReturn(Result.SUCCESS);
        given(firstRun.isBuilding()).willReturn(true);

        Result result = buildUtils.findPreviousBuildResult(run);

        assertThat(result).isNull();
    }

    private void setupMocks() throws Exception {
        given(run.getEnvironment(any(TaskListener.class))).willReturn(new EnvVars());
        given(run.getParent()).willReturn(mockProject);
        given(build.getEnvironment(any(TaskListener.class))).willReturn(new EnvVars());
        given(build.getParent()).willReturn(mockProject);
        given(mockProject.getParent()).willReturn(mockItemGroup);
        given(mockItemGroup.getFullDisplayName()).willReturn("");
    }

    @Test
    public void collectedParametersContainBuildVariablesForAbstractBuild() throws Exception {
        setupMocks();
        Map<String, String> map = Maps.newHashMap();
        map.put("build", "param");
        given(build.getBuildVariables()).willReturn(map);

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, build);

        assertThat(collected).containsEntry("build", "param");
    }

    @Test
    public void collectedParametersContainEnvironmentVariables() throws Exception {
        setupMocks();
        EnvVars envVars = new EnvVars();
        envVars.put("env", "var");
        given(run.getEnvironment(any(TaskListener.class))).willReturn(envVars);
        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("env", "var");
    }

    @Test
    public void collectedParametersContainDurationFromBuild() throws Exception {
        setupMocks();
        given(run.getDurationString()).willReturn("3,9 sec");

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("DURATION", "3,9 sec");
    }

    @Test
    public void collectedParametersContainCorrectUrl() throws Exception {
        setupMocks();

        given(jenkins.getRootUrl()).willReturn("http://localhost:8080/jenkins");
        given(run.getUrl()).willReturn("/job/test%20project/1/");

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("URL", "http://localhost:8080/jenkins/job/test%20project/1/");
    }

    @Test
    public void collectedParametersContainCause() throws Exception {
        setupMocks();
        CauseAction mockAction = mock(CauseAction.class);
        given(mockAction.getShortDescription()).willReturn("buildCause");
        given(run.getAction(eq(CauseAction.class))).willReturn(mockAction);

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("CAUSE", "buildCause");
    }

    @Test
    public void changesOrCauseContainsCauseForRun() throws Exception {
        setupMocks();
        CauseAction mockAction = mock(CauseAction.class);
        given(mockAction.getShortDescription()).willReturn("buildCause");
        given(run.getAction(eq(CauseAction.class))).willReturn(mockAction);

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("CHANGES_OR_CAUSE", "buildCause");
    }

    @Test
    public void changesOrCauseContainsChangesForAbstractBuild() throws Exception {
        setupMocks();
        CauseAction mockAction = mock(CauseAction.class);
        given(mockAction.getShortDescription()).willReturn("buildCause");
        given(run.getAction(eq(CauseAction.class))).willReturn(mockAction);

        setupChangesMock();

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, build);

        String changesOrCause = collected.get("CHANGES_OR_CAUSE");
        assertThat(changesOrCause).isNotNull().isNotEmpty().contains("alice", "bob", "42");
    }

    private void setupChangesMock() {
        given(build.hasChangeSetComputed()).willReturn(true);
        User mockUser = mock(User.class);
        given(mockUser.getDisplayName()).willReturn("alice");

        ChangeLogSet.Entry mockEntry = mock(ChangeLogSet.Entry.class);
        given(mockEntry.getAuthor()).willReturn(mockUser);
        Collection mockList = mock(List.class);
        given(mockList.size()).willReturn(20);
        given(mockEntry.getAffectedFiles()).willReturn(mockList);

        mockUser = mock(User.class);
        given(mockUser.getDisplayName()).willReturn("bob");
        ChangeLogSet.Entry secondMockEntry = mock(ChangeLogSet.Entry.class);
        given(secondMockEntry.getAuthor()).willReturn(mockUser);
        mockList = mock(List.class);
        given(mockList.size()).willReturn(22);
        given(secondMockEntry.getAffectedFiles()).willReturn(mockList);

        given(build.getChangeSet()).willReturn(new FakeChangeLogSet(mockEntry, secondMockEntry));
    }

    @Test
    public void collectedParametersContainChangesForAbstractBuild() throws Exception {
        setupMocks();
        setupChangesMock();
        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, build);

        String changes = collected.get("CHANGES");
        assertThat(changes).isNotNull().isNotEmpty().contains("alice", "bob", "42");
    }

    @Test
    public void collectedParametersContainJobDisplayName() throws Exception {
        setupMocks();
        given(mockProject.getDisplayName()).willReturn("test project");

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("JOB_DISPLAY_NAME", "test project");
    }

    @Test
    public void collectedParametersContainTestDetails() throws Exception {
        setupMocks();
        given(mockTestResults.getFailCount()).willReturn(13);
        given(mockTestResults.getTotalCount()).willReturn(21);
        given(run.getAction(eq(AbstractTestResultAction.class))).willReturn(mockTestResults);

        Map<String, String> collected = buildUtils.collectParametersFor(jenkins, run);

        assertThat(collected).containsEntry("FAILED_TEST_COUNT", "13");
        assertThat(collected).containsEntry("TEST_COUNT", "21");
    }

    private class FakeChangeLogSet extends ChangeLogSet {

        private final Entry[] entries;

        private FakeChangeLogSet(Entry... entries) {
            super(null);
            this.entries = entries;
        }

        @Override
        public boolean isEmptySet() {
            return true;
        }

        @Override
        public Iterator<Entry> iterator() {
            return Arrays.asList(entries).iterator();
        }
    }
}
