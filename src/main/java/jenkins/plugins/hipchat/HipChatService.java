package jenkins.plugins.hipchat;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import org.apache.commons.httpclient.HttpClient;

public abstract class HipChatService {

    /**
     * HTTP Connection timeout when making calls to HipChat
     */
    private static final Integer DEFAULT_TIMEOUT = 10000;

    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(DEFAULT_TIMEOUT);
        client.getHttpConnectionManager().getParams().setSoTimeout(DEFAULT_TIMEOUT);

        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;

            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            }
        }

        return client;
    }

    public abstract void publish(String message, String color) throws NotificationException;

    public abstract void publish(String message, String color, boolean notify) throws NotificationException;
}
