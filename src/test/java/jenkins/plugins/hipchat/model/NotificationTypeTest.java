package jenkins.plugins.hipchat.model;

import static org.assertj.core.api.Assertions.*;

import jenkins.plugins.hipchat.Messages;
import org.junit.Test;

public class NotificationTypeTest {

    @Test
    public void testAbortedStatus() {
        assertThat(NotificationType.ABORTED.getStatus().contains(Messages.Aborted()));
    }

    @Test
    public void testBackToNormalStatus() {
        assertThat(NotificationType.BACK_TO_NORMAL.getStatus().contains(Messages.BackToNormal()));
    }

    @Test
    public void testFailureStatus() {
        assertThat(NotificationType.FAILURE.getStatus().contains(Messages.Failure()));
    }

    @Test
    public void testNotBuiltStatus() {
        assertThat(NotificationType.NOT_BUILT.getStatus().contains(Messages.NotBuilt()));
    }

    @Test
    public void testStartedStatus() {
        assertThat(NotificationType.STARTED.getStatus().contains(Messages.Started()));
    }

    @Test
    public void testSuccessStatus() {
        assertThat(NotificationType.SUCCESS.getStatus().contains(Messages.Success()));
    }

    @Test
    public void testUnstableStatus() {
        assertThat(NotificationType.UNSTABLE.getStatus().contains(Messages.Unstable()));
    }
}
