package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import hudson.Messages;
import hudson.model.AbstractBuild;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildDurationMacroTest {

    @Mock
    private AbstractBuild<?, ?> build;
    private BuildDurationMacro macro = new BuildDurationMacro();

    @Test
    public void shouldReturnBuildDuration() {
        given(build.getDuration()).willReturn(39000l);
        String result = macro.evaluate(build, null, BUILD_DURATION, null, null);

        assertThat(result).isEqualTo(Messages.Util_second(39));
    }

    @Test
    public void shouldReturnBuildDurationForUnfinishedBuilds() {
        given(build.getDuration()).willReturn(0l);
        Whitebox.setInternalState(build, "timestamp", System.currentTimeMillis() - 5000l);
        String result = macro.evaluate(build, null, BUILD_DURATION, null, null);
    
        assertThat(result).isNotEqualTo(Messages.Util_millisecond(0));
    }
}
