package jenkins.plugins.hipchat.ext.tokens;

import static org.mockito.BDDMockito.*;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.mockito.Mock;

public class AbstractChangeLogMacroTest {

    @Mock
    protected AbstractBuild<?, ?> build;

    @Before
    public void setup() {
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
        given(secondMockEntry.getMsgEscaped()).willReturn("&lt;strong&gt;foo&lt;/strong&gt;\n\nMore info about fix");
        given(secondMockEntry.getMsg()).willReturn("<strong>foo</strong>");

        given(build.getChangeSet()).willReturn(new FakeChangeLogSet(mockEntry, secondMockEntry));
    }

    protected class FakeChangeLogSet extends ChangeLogSet {

        private final Entry[] entries;

        protected FakeChangeLogSet(Entry... entries) {
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
