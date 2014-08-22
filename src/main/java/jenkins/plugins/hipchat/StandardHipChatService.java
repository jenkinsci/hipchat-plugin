package jenkins.plugins.hipchat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

public class StandardHipChatService implements HipChatService {

    private static final Logger logger = Logger.getLogger(StandardHipChatService.class.getName());

    /*
     * We want a default value for the hostname to point to the standard
     * HipChat server to ensure that people don't have to reconfigure
     * their Jenkins instances to retain default behavior.
     */
    private String host = "api.hipchat.com";
    private String token;
    private String[] roomIds;
    // Setting a default value for the from user.
    private String from = "Build Server";

    public StandardHipChatService(String host, String token, String roomId, String from) {
        super();
        // Check for null value to avoid overwriting the default
        if (host != null) {
            this.host = host;
        }
        this.token = token;
        //
        // If roomId is left blank let's make it an empty string array to
        // avoid throwing a nasty NullPointerException during job run if this
        // is unset.
        //
        if (roomId != null) {
            this.roomIds = roomId.split(",");
        } else {
            this.roomIds = new String[0];
        }
        // Check for null value to avoid overwriting the default
        if (from != null) {
            this.from = from;
        }
    }

    public void publish(String message) {
        publish(message, "yellow");
    }

    public void publish(String message, String color) {
        logger.info("HipChat notifications will be sent to HipChat server: " + host);
        for (String roomId : roomIds) {
            logger.info("Posting: " + from + " to " + roomId + ": " + message + " " + color);
            HttpClient client = getHttpClient();
            String url = "https://" + host + "/v1/rooms/message?auth_token=" + token;
            PostMethod post = new PostMethod(url);

            try {
                post.addParameter("from", from);
                post.addParameter("room_id", roomId);
                post.addParameter("message", message);
                post.addParameter("color", color);
                post.addParameter("notify", shouldNotify(color));
                post.getParams().setContentCharset("UTF-8");
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_OK || ! response.contains("\"sent\"")) {
                    logger.log(Level.WARNING, "HipChat post may have failed. Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to HipChat", e);
            } finally {
                post.releaseConnection();
            }
        }
        //
        // Let's print out a message letting people know the roomIds were blank just to avoid confusion
        //
        if (roomIds.length <= 0) {
            logger.info("No rooms were configured for this job, so no notifications were sent.");
        }
    }
    
    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            }
        }
        return client;
    }

    private String shouldNotify(String color) {
        return color.equalsIgnoreCase("green") ? "0" : "1";
    }

    void setHost(String host) {
        if (host != null) {
            this.host = host;
        }
    }
}
