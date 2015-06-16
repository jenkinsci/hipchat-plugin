package jenkins.plugins.hipchat;

import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public abstract class HipChatService {

    /**
     * HTTP Connection timeout when making calls to HipChat
     */
    public static final Integer DEFAULT_TIMEOUT = 10000;
    protected static final Integer DEFAULT_PORT = 443;
    protected static final String SCHEME = "https";

    protected HttpClient getHttpClient() {
        ProtocolSocketFactory customFactory = new TLSProtocolSocketFactory();
        Protocol customHttps = new Protocol(SCHEME, customFactory, DEFAULT_PORT);
        Protocol.registerProtocol(SCHEME, customHttps);
        
        HttpClient client = new HttpClient();
        HttpConnectionManagerParams params = client.getHttpConnectionManager().getParams();
        params.setConnectionTimeout(DEFAULT_TIMEOUT);
        params.setSoTimeout(DEFAULT_TIMEOUT);

        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;

            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            }
        }

        return client;
    }

    public abstract void publish(String message, String color);

    public abstract void publish(String message, String color, boolean notify);
}
