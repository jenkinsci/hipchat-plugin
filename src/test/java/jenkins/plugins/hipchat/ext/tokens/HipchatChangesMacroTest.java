package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import hudson.model.AbstractProject;
import hudson.model.CauseAction;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HipchatChangesMacroTest extends AbstractChangeLogMacroTest {

    @Mock
    private AbstractProject project;
    @Mock
    private ItemGroup itemGroup;
    private final HipchatChangesMacro macro = new HipchatChangesMacro();

    @Test
    public void shouldReturnChangesCorrectly() {
        String result = macro.evaluate(build, null, CHANGES, null, null);
        assertThat(result).isNotNull().isNotEmpty().contains("alice", "bob", "42");
    }

    @Test
    public void shouldNotFailIfAffectedFilesCannotBeDetermined() {
        given(build.getParent()).willReturn(project);
        given(project.getParent()).willReturn(itemGroup);
        given(itemGroup.getFullDisplayName()).willReturn("");
        given(build.hasChangeSetComputed()).willReturn(true);
        User mockUser = mock(User.class);
        given(mockUser.getDisplayName()).willReturn("alice");
        ChangeLogSet.Entry mockEntry = mock(ChangeLogSet.Entry.class);
        given(mockEntry.getAuthor()).willReturn(mockUser);
        given(mockEntry.getAffectedFiles()).willThrow(UnsupportedOperationException.class);
        given(build.getChangeSet()).willReturn(new FakeChangeLogSet(mockEntry));

        String result = macro.evaluate(build, null, CHANGES, null, null);

        assertThat(result).isNotNull().isNotEmpty().contains("alice", "0");
    }

    @Test
    public void changesOrCauseContainsChangesForAbstractBuild() {
        String result = macro.evaluate(build, null, CHANGES_OR_CAUSE, null, null);

        assertThat(result).isNotNull().isNotEmpty().contains("alice", "bob", "42");
    }

    @Test
    public void changesOrCauseReturnsCauseIfChangesAreNotFound() {
        given(build.getParent()).willReturn(project);
        given(project.getParent()).willReturn(itemGroup);
        given(itemGroup.getFullDisplayName()).willReturn("");
        given(build.hasChangeSetComputed()).willReturn(false);
        CauseAction mockAction = mock(CauseAction.class);
        given(mockAction.getShortDescription()).willReturn("buildCause");
        given(build.getAction(eq(CauseAction.class))).willReturn(mockAction);

        String result = macro.evaluate(build, null, CHANGES_OR_CAUSE, null, null);

        assertThat(result).isNotNull().isNotEmpty().contains("buildCause");
    }
}
