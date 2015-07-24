package jenkins.plugins.hipchat.ext;

import hudson.ProxyConfiguration;
import java.net.Proxy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

public class ProxyRoutePlanner extends DefaultRoutePlanner {

    private final ProxyConfiguration proxyConfiguration;

    public ProxyRoutePlanner(ProxyConfiguration proxyConfiguration) {
        super(DefaultSchemePortResolver.INSTANCE);
        this.proxyConfiguration = proxyConfiguration;
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        Proxy proxy = proxyConfiguration.createProxy(target.getHostName());
        if (Proxy.Type.DIRECT.equals(proxy.type())) {
            return null;
        }

        return new HttpHost(proxyConfiguration.name, proxyConfiguration.port);
    }
}
