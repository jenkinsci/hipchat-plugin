package jenkins.plugins.hipchat.impl;

import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.hipchat.HipChatService;
import jenkins.plugins.hipchat.Messages;
import jenkins.plugins.hipchat.exceptions.InvalidResponseCodeException;
import jenkins.plugins.hipchat.exceptions.NotificationException;

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
            HttpClient client = getHttpClient();
            String url = "https://" + server + "/v1/rooms/message";
            PostMethod post = new PostMethod(url);

            try {
                post.addParameter("auth_token", token);
                post.addParameter("from", sendAs);
                post.addParameter("room_id", roomId);
                post.addParameter("message", message);
                post.addParameter("color", color);
                post.addParameter("notify", notify ? "1" : "0");
                post.getParams().setContentCharset("UTF-8");
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if (responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "HipChat post may have failed. ResponseCode: {0}, Response: {1}",
                            new Object[]{responseCode, response});
                    throw new InvalidResponseCodeException(responseCode);
                }
            } catch (IllegalArgumentException iae) {
                logger.log(Level.WARNING, "Invalid argument provided", iae);
                throw new NotificationException(Messages.IllegalArgument(iae.getMessage()));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
                throw new NotificationException(Messages.IOException(ioe.getMessage()));
            } finally {
                post.releaseConnection();
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
