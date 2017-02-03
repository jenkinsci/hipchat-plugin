package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CommitMessageMacroTest extends AbstractChangeLogMacroTest {

    private CommitMessageMacro macro;

    @Before
    public void configure() {
        macro = new CommitMessageMacro();
    }

    @Test
    public void shouldReturnCommitMessageEscaped() {
        String result = macro.evaluate(build, null, COMMIT_MESSAGE);

        assertThat(result).isNotNull().isEqualTo("&lt;strong&gt;foo&lt;/strong&gt;");
    }

    @Test
    public void shouldReturnCommitMessageAsIsWhenEscapingIsDisabled() {
        macro.escape = false;

        String result = macro.evaluate(build, null, COMMIT_MESSAGE);

        assertThat(result).isNotNull().isEqualTo("<strong>foo</strong>");
    }
}
