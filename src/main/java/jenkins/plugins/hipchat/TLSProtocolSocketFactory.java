package jenkins.plugins.hipchat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;

/**
 * Patterned after https://discretemkt.wordpress.com/2014/11/16/commons-httpclient-can-disable-sslv3/
 */
public class TLSProtocolSocketFactory extends SSLProtocolSocketFactory {
    
    public TLSProtocolSocketFactory() {
        super();
    }
    
    /**
     * This is to enable TLSv1.1/2, which are disabled by default in Java 7, and disable SSLv2/3
     * http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html
     */
    private Socket stripSSLv3(Socket socket) {
        if (!(socket instanceof SSLSocket))
            return socket;
        SSLSocket sslSocket = (SSLSocket) socket;
        List<String> list = new ArrayList<String>();
        for (String s : sslSocket.getSupportedProtocols())
            if (!s.startsWith("SSLv2") && !s.startsWith("SSLv3"))
                list.add(s);
        sslSocket.setEnabledProtocols(list.toArray(new String[list.size()]));
        return sslSocket;
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = super.createSocket(host, port);
        return stripSSLv3(socket);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = super.createSocket(host, port, localAddress, localPort);
        return stripSSLv3(socket);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException {
        Socket socket = super.createSocket(host, port, localAddress, localPort, params);
        return stripSSLv3(socket);
    }
    
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        Socket newSocket = super.createSocket(socket, host, port, autoClose);
        return stripSSLv3(newSocket);
    }
    
}