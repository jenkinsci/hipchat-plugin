package jenkins.plugins.hipchat;

import hudson.ProxyConfiguration;
import hudson.Util;
import java.io.Closeable;
import java.io.IOException;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.exceptions.NotificationException;
import jenkins.plugins.hipchat.ext.ProxyRoutePlanner;
import jenkins.plugins.hipchat.ext.TLSSocketFactory;
import jenkins.plugins.hipchat.model.notifications.Notification;
import jenkins.plugins.hipchat.model.notifications.Notification.Color;
import jenkins.plugins.hipchat.model.notifications.Notification.MessageFormat;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.util.EntityUtils;

public abstract class HipChatService {

    /**
     * HTTP Connection timeout when making calls to HipChat.
     */
    private static final Integer DEFAULT_TIMEOUT = 10000;

    protected CloseableHttpClient getHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).build())
                .setSSLSocketFactory(new TLSSocketFactory());

        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;

            if (proxy != null) {
                httpClientBuilder.setRoutePlanner(new ProxyRoutePlanner(proxy));
                if (Util.fixEmpty(proxy.getUserName()) != null) {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(proxy.name, proxy.port),
                            new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                }
            }
        }

        return httpClientBuilder.build();
    }

    /**
     * Publishes a notification to HipChat.
     *
     * @param message The message to send.
     * @param color The notification color to use.
     * @throws NotificationException If there was an error while publishing the notification.
     * @deprecated This method currently does not expose all the available HipChat functionalities, use
     * {@link #publish(jenkins.plugins.hipchat.model.notifications.Notification)} instead.
     */
    @Deprecated
    public final void publish(String message, String color) throws NotificationException {
        publish(message, color, !color.equalsIgnoreCase("green"));
    }

    public void publish(String message, String color, boolean notify) throws NotificationException {
        publish(message, color, notify, false);
    }

    public void publish(String message, String color, boolean notify, boolean textFormat) throws NotificationException {
        publish(new Notification()
                .withMessage(message)
                .withColor(Color.fromValue(color))
                .withNotify(notify)
                .withMessageFormat(textFormat ? MessageFormat.TEXT : MessageFormat.HTML));
    }

    public abstract void publish(Notification notification) throws NotificationException;

    protected final String readResponse(HttpEntity entity) throws IOException {
        return entity != null ? EntityUtils.toString(entity) : null;
    }

    protected final void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }
}
