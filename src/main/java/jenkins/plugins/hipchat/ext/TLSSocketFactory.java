package jenkins.plugins.hipchat.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;

public class TLSSocketFactory extends SSLConnectionSocketFactory {

    public TLSSocketFactory() {
        super(SSLContexts.createDefault(), getDefaultHostnameVerifier());
    }

    @Override
    protected void prepareSocket(SSLSocket socket) throws IOException {
        String[] supportedProtocols = socket.getSupportedProtocols();
        List<String> protocols = new ArrayList<String>(5);
        for (String supportedProtocol : supportedProtocols) {
            if (!supportedProtocol.startsWith("SSL")) {
                protocols.add(supportedProtocol);
            }
        }
        socket.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));
    }
}
