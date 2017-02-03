package jenkins.plugins.hipchat.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import hudson.model.Result;
import hudson.model.Run;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildUtilsTest {

    @Mock
    private Run<?, ?> run;
    @Mock
    private Run firstRun;
    @Mock
    private Run secondRun;
    @Mock
    private Run thirdRun;
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
}
