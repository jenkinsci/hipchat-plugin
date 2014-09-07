package jenkins.plugins.hipchat;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StandardHipChatService implements HipChatService {
    private static final Logger logger = Logger.getLogger(StandardHipChatService.class.getName());
    private static final String DEFAULT_HOST = "api.hipchat.com";
    private static final String[] DEFAULT_ROOMS = new String[0];
    private static final String DEFAULT_FROM = "Build Server";

    private String host;
    private String token;
    private String[] roomIds;
    private String from;
    private Boolean v2API;

    public StandardHipChatService(String host, String token, String roomIds, String from, Boolean v2API) {
        super();
        this.host = host == null ? DEFAULT_HOST : host;
        this.token = token;
        this.roomIds = roomIds == null ? DEFAULT_ROOMS : roomIds.split("\\s*,\\s*");
        this.from = from == null ? DEFAULT_FROM : from;
        this.v2API = v2API == null ? false : v2API;
    }

    public void publish(String message) {
        publish(message, "yellow");
    }

    public void publish(String message, String color) {
        for (String roomId : roomIds) {
            logger.info("Posting: " + from + " to " + roomId + ": " + message + " " + color);
            HttpClient client = getHttpClient();

            PostMethod post = null;
            try {
                if (v2API) {
                    post = buildV2Notification(message, color, roomId);
                } else {
                    post = buildV1Notification(message, color, roomId);
                }
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if (!v2API && (responseCode != HttpStatus.SC_OK || !response.contains("\"sent\""))
                        || (v2API && responseCode != HttpStatus.SC_NO_CONTENT)) {
                    logger.log(Level.WARNING, "HipChat post may have failed. Response Code: " + responseCode + ". Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to HipChat", e);
            } finally {
                if (post != null) {
                    post.releaseConnection();
                }
            }
        }
    }

    private PostMethod buildV1Notification(String message, String color, String roomId) {
        String url = "https://" + host + "/v1/rooms/message?auth_token=" + token;
        PostMethod post = new PostMethod(url);
        post.addParameter("from", from);
        post.addParameter("room_id", roomId);
        post.addParameter("message", message);
        post.addParameter("color", color);
        post.addParameter("notify", shouldNotifyAsString(color));
        post.getParams().setContentCharset("UTF-8");
        return post;
    }

    private PostMethod buildV2Notification(String message, String color, String roomId)
            throws UnsupportedEncodingException {
        String url = "https://" + host + "/v2/room/" + roomId + "/notification?auth_token=" + token;

        JSONObject postJSON = new JSONObject();
        postJSON.put("message", message);
        postJSON.put("color", color);
        postJSON.put("notify", shouldNotify(color));

        StringRequestEntity requestEntity = new StringRequestEntity(postJSON.toString(), "application/json", "UTF-8");
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(requestEntity);
        return post;
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

    private String shouldNotifyAsString(String color) {
        return color.equalsIgnoreCase("green") ? "0" : "1";
    }

    private Boolean shouldNotify(String color) {
        return color.equalsIgnoreCase("green");
    }

    public String getHost() {
        return host;
    }

    public String[] getRoomIds() {
        return roomIds;
    }

    public String getFrom() {
        return from;
    }

    public Boolean isV2API() {
        return v2API;
    }
}
