package jenkins.plugins.hipchat.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import hudson.Util;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.InvalidResponseCodeException;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import jenkins.plugins.hipchat.model.notifications.Notification;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

public class HipChatV2Service extends HipChatService {

    private static final Logger LOGGER = Logger.getLogger(HipChatV2Service.class.getName());
    private static final String[] DEFAULT_ROOMS = new String[0];
    private static final int MAX_MESSAGE_LENGTH = 10000;
    private static final ObjectWriter writer = new ObjectMapper().writerWithView(Notification.class);

    private final String server;
    private final String token;
    private final String[] roomIds;

    public HipChatV2Service(String server, String token, String roomIds) {
        this.server = server;
        this.token = token;
        this.roomIds = roomIds == null ? DEFAULT_ROOMS : roomIds.split("\\s*,\\s*");
    }

    @Override
    public void publish(Notification notification) throws NotificationException {
        if (notification.getMessage().length() > MAX_MESSAGE_LENGTH) {
            LOGGER.log(Level.INFO, "HipChat notification message was too long, truncating to maximum message length");
            notification.setMessage(notification.getMessage().substring(0, MAX_MESSAGE_LENGTH - 3) + "...");
        }
        for (String roomId : roomIds) {
            LOGGER.log(Level.FINE, "Posting to {0} room: {1}", new Object[]{roomId, notification});
            CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse httpResponse = null;

            try {
                HttpPost post = new HttpPost("https://" + server + "/v2/room/" + Util.rawEncode(roomId)
                        + "/notification");
                post.addHeader("Authorization", "Bearer " + token);
                post.setEntity(new StringEntity(writer.writeValueAsString(notification), ContentType.APPLICATION_JSON));

                httpResponse = httpClient.execute(post);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                // Always read response to ensure the inputstream is closed
                String response = readResponse(httpResponse.getEntity());

                if (responseCode != 204) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, "HipChat post may have failed. ResponseCode: {0}, Response: {1}",
                                new Object[]{responseCode, response});
                        throw new InvalidResponseCodeException(responseCode);
                    }
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
                throw new NotificationException(Messages.IOException(ioe.toString()));
            } finally {
                closeQuietly(httpResponse, httpClient);
            }
        }
    }
}
