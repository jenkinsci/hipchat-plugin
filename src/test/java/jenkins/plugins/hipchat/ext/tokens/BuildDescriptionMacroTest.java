package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import hudson.model.Run;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildDescriptionMacroTest {

    @Mock
    private Run<?, ?> run;
    private BuildDescriptionMacro macro = new BuildDescriptionMacro();

    @Test
    public void shouldReturnBuildDuration() {
        given(run.getDescription()).willReturn("hello world description");
        String result = macro.evaluate(run, null, null, BUILD_DESCRIPTION, null, null);

        assertThat(result).isEqualTo("hello world description");
    }
}
