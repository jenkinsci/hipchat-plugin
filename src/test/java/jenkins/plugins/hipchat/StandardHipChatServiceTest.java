package jenkins.plugins.hipchat;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;

public class StandardHipChatServiceTest {
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardHipChatService service = new StandardHipChatService("badhost", "token", "room", "from", false);
        service.publish("message");
    }

    @Test
    public void shouldSetADefaultHost() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", "from", false);
        assertEquals("api.hipchat.com", service.getHost());
    }

    @Test
    public void shouldBeAbleToOverrideHost() {
        StandardHipChatService service = new StandardHipChatService("some.other.host", "token", "room", "from", false);
        assertEquals("some.other.host", service.getHost());
    }

    @Test
    public void shouldSplitTheRoomIds() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room1,room2", "from", false);
        assertArrayEquals(new String[]{"room1", "room2"}, service.getRoomIds());
    }

    @Test
    public void shouldTrimTheRoomIds() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room1, room2", "from", false);
        assertArrayEquals(new String[]{"room1", "room2"}, service.getRoomIds());
    }

    @Test
    public void shouldNotSplitTheRoomsIfNullIsPassed() {
        StandardHipChatService service = new StandardHipChatService(null, "token", null, "from", false);
        assertArrayEquals(new String[0], service.getRoomIds());
    }

    @Test
    public void shouldProvideADefaultFrom() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", null, false);
        assertEquals("Build Server", service.getFrom());
    }

    @Test
    public void shouldBeAbleToOverrideFrom() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", "from", false);
        assertEquals("from", service.getFrom());
    }

    @Test
    public void shouldOverrideAPIToVersion2() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", "from", true);
        assertTrue(service.isV2API());
    }

    @Test
    public void shouldAPIDefaultToVersion1() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", "from", false);
        assertFalse(service.isV2API());
    }
}
