package jenkins.plugins.hipchat.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.InvalidResponseCodeException;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HipChatV1Service extends HipChatService {

    private static final Logger logger = Logger.getLogger(HipChatV1Service.class.getName());
    private static final String[] DEFAULT_ROOMS = new String[0];

    private final String server;
    private final String token;
    private final String[] roomIds;
    private final String sendAs;

    public HipChatV1Service(String server, String token, String roomIds, String sendAs) {
        this.server = server;
        this.token = token;
        this.roomIds = roomIds == null ? DEFAULT_ROOMS : roomIds.split("\\s*,\\s*");
        this.sendAs = sendAs;
    }

    @Override
    public void publish(String message, String color) throws NotificationException {
        publish(message, color, shouldNotify(color));
    }

    @Override
    public void publish(String message, String color, boolean notify) throws NotificationException {
        for (String roomId : roomIds) {
            logger.log(Level.FINE, "Posting: {0} to {1}: {2} {3}", new Object[]{sendAs, roomId, message, color});
            CloseableHttpClient httpClient = getHttpClient();
            CloseableHttpResponse httpResponse = null;

            try {
                HttpPost post = new HttpPost("https://" + server + "/v1/rooms/message");
                List<NameValuePair> nvps = new ArrayList<NameValuePair>(6);
                nvps.add(new BasicNameValuePair("auth_token", token));
                nvps.add(new BasicNameValuePair("from", sendAs));
                nvps.add(new BasicNameValuePair("room_id", roomId));
                nvps.add(new BasicNameValuePair("message", message));
                nvps.add(new BasicNameValuePair("color", color));
                nvps.add(new BasicNameValuePair("notify", notify ? "1" : "0"));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

                httpResponse = httpClient.execute(post);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                // Always read response to ensure the inputstream is closed
                String response = readResponse(httpResponse.getEntity());

                if (responseCode != 200) {
                    logger.log(Level.WARNING, "HipChat post may have failed. ResponseCode: {0}, Response: {1}",
                            new Object[]{responseCode, response});
                    throw new InvalidResponseCodeException(responseCode);
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
                throw new NotificationException(Messages.IOException(ioe.toString()));
            } finally {
                closeQuietly(httpResponse, httpClient);
            }
        }
    }

    private boolean shouldNotify(String color) {
        return !color.equalsIgnoreCase("green");
    }

    public String getServer() {
        return server;
    }

    public String[] getRoomIds() {
        return roomIds;
    }

    public String getSendAs() {
        return sendAs;
    }
}
