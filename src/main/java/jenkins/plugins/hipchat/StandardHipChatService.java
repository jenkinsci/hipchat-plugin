package jenkins.plugins.hipchat;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StandardHipChatService implements HipChatService {

    private static final Logger logger = Logger.getLogger(StandardHipChatService.class.getName());
    private static final String[] DEFAULT_ROOMS = new String[0];

    private final HttpClient httpClient;
    private final String server;
    private final String token;
    private final String[] roomIds;
    private final String sendAs;

    StandardHipChatService(HttpClient httpClient, String server, String token, String roomIds, String sendAs) {
        super();
        this.httpClient = httpClient;
        this.server = server;
        this.token = token;
        this.roomIds = roomIds == null ? DEFAULT_ROOMS : roomIds.split("\\s*,\\s*");
        this.sendAs = sendAs;
    }

    public StandardHipChatService(String server, String token, String roomIds, String sendAs) {
        this(null, server, token, roomIds, sendAs);
    }

    public void publish(String message) {
        publish(message, "yellow");
    }

    public void publish(String message, String color) {
        for (String roomId : roomIds) {
            logger.log(Level.INFO, "Posting: {0} to {1}: {2} {3}", new Object[]{sendAs, roomId, message, color});
            HttpClient client = getHttpClient();
            String url = "https://" + server + "/v1/rooms/message?auth_token=" + token;
            PostMethod post = new PostMethod(url);

            try {
                post.addParameter("from", sendAs);
                post.addParameter("room_id", roomId);
                post.addParameter("message", message);
                post.addParameter("color", color);
                post.addParameter("notify", shouldNotify(color));
                post.getParams().setContentCharset("UTF-8");
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if (responseCode != HttpStatus.SC_OK || !response.contains("\"sent\"")) {
                    logger.log(Level.WARNING, "HipChat post may have failed. Response: {0}", response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to HipChat", e);
            } finally {
                post.releaseConnection();
            }
        }
    }

    public String getMentionNameForEmail(String email) {
        HttpClient client = getHttpClient();
        String url = "https://" + server + "/v1/users/show";
        GetMethod get = new GetMethod(url);
        get.setQueryString(new NameValuePair[] {
            new NameValuePair("user_id", email),
            new NameValuePair("auth_token", token)
        });

        try {
            int responseCode = client.executeMethod(get);
            if (responseCode == HttpStatus.SC_OK) {
                return parseMentionNameFromUserResponse(get.getResponseBodyAsString());
            } else {
                logger.log(Level.WARNING, "HipChat user lookup has failed with error: {0}", get.getResponseBodyAsString());
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error looking up user from HipChat", e);
            return null;
        } finally {
            get.releaseConnection();
        }
    }

    private HttpClient getHttpClient() {
        if (httpClient != null) {
            return httpClient;
        } else {
            HttpClient client = new HttpClient();
    
            if (Jenkins.getInstance() != null) {
                ProxyConfiguration proxy = Jenkins.getInstance().proxy;
    
                if (proxy != null) {
                    client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                }
            }
    
            return client;
        }
    }

    private String shouldNotify(String color) {
        return color.equalsIgnoreCase("green") ? "0" : "1";
    }

    private String parseMentionNameFromUserResponse(String response) {
        return JSONObject.fromObject(response).getJSONObject("user").getString("mention_name");
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
