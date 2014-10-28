package jenkins.plugins.hipchat;

import hudson.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.*;

public class StandardHipChatServiceTest {
    private HttpClient httpClient;
    private StandardHipChatService standardHipChatService;

    @Before
    public void setUp() {
        httpClient = mock(HttpClient.class);
        standardHipChatService = new StandardHipChatService("api.hipchat.com", "token", "room", "from");
    }

    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardHipChatService service = new StandardHipChatService("badhost", "token", "room", "from");
        service.publish("message");
    }

    @Test
    public void shouldBeAbleToOverrideHost() {
        StandardHipChatService service = new StandardHipChatService("some.other.host", "token", "room", "from");
        assertEquals("some.other.host", service.getServer());
    }

    @Test
    public void shouldSplitTheRoomIds() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room1,room2", "from");
        assertArrayEquals(new String[]{"room1", "room2"}, service.getRoomIds());
    }

    @Test
    public void shouldTrimTheRoomIds() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room1, room2", "from");
        assertArrayEquals(new String[]{"room1", "room2"}, service.getRoomIds());
    }

    @Test
    public void shouldNotSplitTheRoomsIfNullIsPassed() {
        StandardHipChatService service = new StandardHipChatService(null, "token", null, "from");
        assertArrayEquals(new String[0], service.getRoomIds());
    }

    @Test
    public void shouldBeAbleToOverrideFrom() {
        StandardHipChatService service = new StandardHipChatService(null, "token", "room", "from");
        assertEquals("from", service.getSendAs());
    }

    @Test
    public void shouldReturnMentionNameBasedOnEmail() throws HttpException, IOException {
        givenTheRequestForQueryStringReturns("user_id=bob%40geldof.com&auth_token=token",
                "{\"user\":{\"mention_name\":\"BobbieG\"}}");
        String mentionName = standardHipChatService.getMentionNameForEmail("bob@geldof.com");
        assertEquals("BobbieG", mentionName);
    }

    private void givenTheRequestForQueryStringReturns(final String queryString, final String response)
            throws HttpException, IOException {
        when(httpClient.executeMethod(any(GetMethod.class))).thenAnswer(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                GetMethod get = invocation.getArgumentAt(0, GetMethod.class);
                assertEquals(queryString, get.getQueryString());

                Method method = HttpMethodBase.class.getDeclaredMethod("setResponseStream", InputStream.class);
                method.setAccessible(true);
                InputStream in = new ByteArrayInputStream(response.getBytes());
                ReflectionUtils.invokeMethod(method, get, new Object[] {in});

                return HttpStatus.SC_OK;
            }
        });
    }
}
