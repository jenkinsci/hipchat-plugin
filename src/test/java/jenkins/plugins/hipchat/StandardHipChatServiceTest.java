package jenkins.plugins.hipchat;

import hudson.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StandardHipChatServiceTest {
    private HttpClient httpClient;
    private StandardHipChatService standardHipChatService;

    @Before
    public void setUp() {
        httpClient = mock(HttpClient.class);
        standardHipChatService = new StandardHipChatService(httpClient, "api.hipchat.com", "token", "room", "from");
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
    public void shouldReturnMentionNameBasedOnEmail() {
        givenTheRequestForBobGeldofsUserReturnsOk();
        String mentionName = standardHipChatService.getMentionNameForEmail("bob@geldof.com");
        assertEquals("BobbieG", mentionName);
    }

    @Test
    public void shouldReturnNullMentionNameForInvalidResponse() {
        givenTheRequestForRodJaneAndFreddyUserReturnsInvalidJson();
        String mentionName = standardHipChatService.getMentionNameForEmail("rodjanefreddy@rainbow.com");
        assertNull(mentionName);
    }

    @Test
    public void shouldReturnNullMentionNameForErrorResponse() {
        givenTheRequestForBonoReturnsAnErrorResponse();
        String mentionName = standardHipChatService.getMentionNameForEmail("bono@therealu2.com");
        assertNull(mentionName);
    }

    private void givenTheRequestForBobGeldofsUserReturnsOk() {
        mockUpHttpClient(HttpStatus.SC_OK, "user_id=bob%40geldof.com&auth_token=token",
                "{\"user\":{\"mention_name\":\"BobbieG\"}}");
    }
    
    private void givenTheRequestForRodJaneAndFreddyUserReturnsInvalidJson() {
        mockUpHttpClient(HttpStatus.SC_OK, "user_id=rodjanefreddy%40rainbow.com&auth_token=token",
                "RAAAAAAAAAAA");
    }
    
    private void givenTheRequestForBonoReturnsAnErrorResponse() {
        mockUpHttpClient(HttpStatus.SC_FORBIDDEN, "user_id=bono%40therealu2.com&auth_token=token",
                "{\"error\":{\"code\":401}");
    }

    private void mockUpHttpClient(final int responseCode, final String expectedQueryString, final String response) {
        try {
            when(httpClient.executeMethod(any(GetMethod.class))).thenAnswer(new Answer<Integer>() {
                public Integer answer(InvocationOnMock invocation) throws Throwable {
                    GetMethod get = invocation.getArgumentAt(0, GetMethod.class);
                    assertEquals(expectedQueryString, get.getQueryString());
    
                    Method method = HttpMethodBase.class.getDeclaredMethod("setResponseStream", InputStream.class);
                    method.setAccessible(true);
                    InputStream in = new ByteArrayInputStream(response.getBytes());
                    ReflectionUtils.invokeMethod(method, get, new Object[] {in});
    
                    return responseCode;
                }
            });
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
