package jenkins.plugins.hipchat;

import java.util.HashSet;
import java.util.Set;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.Mailer;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

public class ActiveNotifierTest {
    private HipChatService hipChatService;
    private HipChatNotifier hipChatNotifier;
    private ActiveNotifier activeNotifier;
    
    @SuppressWarnings("rawtypes")
    private AbstractProject project;
    @SuppressWarnings("rawtypes")
    private AbstractBuild lastBuild;
    @SuppressWarnings("rawtypes")
    private AbstractBuild build;

    @Before
    public void setUp() {
        hipChatNotifier = mock(HipChatNotifier.class);
        activeNotifier = new ActiveNotifier(hipChatNotifier);

        hipChatService = mock(HipChatService.class);
        when(hipChatNotifier.newHipChatService()).thenReturn(hipChatService);

        project = mock(AbstractProject.class);
        lastBuild = mock(AbstractBuild.class);
        build = mock(AbstractBuild.class);

        when(build.getProject()).thenReturn(project);
        when(project.getLastBuild()).thenReturn(lastBuild);
    }

    @Test
    public void addCulpritsToPublishedMessageWhenEnabled() {
        givenTheBuildHasJustFailedAndWereListeningForFailures();
        givenThereWereThreeCulpritsOneOfWhichIsKnownToHipChat();

        when(hipChatNotifier.isIncludeCulprits()).thenReturn(true);
        activeNotifier.completed(build);

        thenHipChatNotificationShouldContain("Henrys Cat");
        thenHipChatNotificationShouldContain("@KnobCat");
        thenHipChatNotificationShouldContain("grumpy@cat.com");
    }

    private void givenTheBuildHasJustFailedAndWereListeningForFailures() {
        when(build.getResult()).thenReturn(Result.FAILURE);
        when(hipChatNotifier.isNotifyFailure()).thenReturn(true);
        when(build.getUrl()).thenReturn("http://www.cats.com");
    }

    private void givenThereWereThreeCulpritsOneOfWhichIsKnownToHipChat() {
        Set<User> culprits = new HashSet<User>();
        culprits.add(makeUser("Henrys Cat", null));
        culprits.add(makeUser("#KnobCat", "knob@cat.com"));
        culprits.add(makeUser("Grumpy Cat", "grumpy@cat.com"));
        when(build.getCulprits()).thenReturn(culprits);
        
        when(hipChatService.getMentionNameForEmail("knob@cat.com")).thenReturn("KnobCat");
    }

    private void thenHipChatNotificationShouldContain(String contains) {
        verify(hipChatService).publish(argThat(containsString(contains)), anyString());
    }

    private User makeUser(String fullName, String email) {
        User user = mock(User.class);
        when(user.getFullName()).thenReturn(fullName);
        if (email != null) {
            Mailer.UserProperty userProperty = mock(Mailer.UserProperty.class);
            when(userProperty.getAddress()).thenReturn(email);
            when(user.getProperty(Mailer.UserProperty.class)).thenReturn(userProperty);
        }
        return user;
    }
}
