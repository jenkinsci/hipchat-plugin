package jenkins.plugins.hipchat.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.HipChatNotifier.DescriptorImpl;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.impl.NoopCardProvider;
import jenkins.plugins.hipchat.model.notifications.Notification.Color;
import jenkins.plugins.hipchat.utils.BuildUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTypeTest {

    @Mock
    Jenkins jenkins;
    @Mock
    DescriptorImpl descriptor;
    @Mock
    BuildUtils buildUtils;
    @Mock
    AbstractBuild<?, ?> build;

    @Before
    public void setup() {
        given(jenkins.getDescriptorByType(any(Class.class))).willReturn(descriptor);
    }

    @Test
    public void testGetMessage() throws Exception {
        assertNotificationMessage(NotificationType.ABORTED, Messages.Aborted());
        assertNotificationMessage(NotificationType.BACK_TO_NORMAL, Messages.BackToNormal());
        assertNotificationMessage(NotificationType.FAILURE, Messages.Failure());
        assertNotificationMessage(NotificationType.NOT_BUILT, Messages.NotBuilt());
        assertNotificationMessage(NotificationType.STARTED, Messages.Started());
        assertNotificationMessage(NotificationType.SUCCESS, Messages.Success());
        assertNotificationMessage(NotificationType.UNSTABLE, Messages.Unstable());
    }

    private void assertNotificationMessage(NotificationType type, String message) {
        assertThat(type.getNotification(config(type), build, buildUtils, jenkins).getMessage())
                .contains("Hello World " + message);
    }

    private NotificationConfig config(NotificationType type) {
        return new NotificationConfig(true, false, type, Color.RANDOM, "Hello World $STATUS");
    }
}
