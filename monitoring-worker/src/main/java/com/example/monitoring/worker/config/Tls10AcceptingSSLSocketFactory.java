package com.example.monitoring.worker.config;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * TLS 1.0을 포함한 구버전 프로토콜을 허용하는 SSLSocketFactory.
 * SMTP 서버가 TLS 1.0만 지원할 때 사용 (보안상 서버 측 TLS 1.2 이상 권장).
 */
public class Tls10AcceptingSSLSocketFactory extends SSLSocketFactory {

    private static final String[] ENABLED_PROTOCOLS = {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

    private final SSLSocketFactory delegate;

    public Tls10AcceptingSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    private static Socket enableTls10(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket ssl = (SSLSocket) socket;
            String[] supported = ssl.getSupportedProtocols();
            String[] toSet = Arrays.stream(ENABLED_PROTOCOLS)
                    .filter(p -> Arrays.asList(supported).contains(p))
                    .toArray(String[]::new);
            if (toSet.length > 0) {
                ssl.setEnabledProtocols(toSet);
            }
        }
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTls10(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTls10(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTls10(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTls10(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTls10(delegate.createSocket(address, port, localAddress, localPort));
    }
}
