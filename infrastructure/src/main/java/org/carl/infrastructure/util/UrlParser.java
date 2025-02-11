package org.carl.infrastructure.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlParser {

    private String protocol;
    private String host;
    private int port;
    private String path;
    private String query;

    public UrlParser(String url) throws URISyntaxException {
        parseUrl(url);
    }

    private void parseUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);

        // 解析协议
        this.protocol = Protocol.from(uri.getScheme()).name();

        // 解析主机
        this.host = uri.getHost();

        // 解析端口
        this.port = uri.getPort();

        // 解析路径
        this.path = uri.getPath();

        // 解析查询参数
        this.query = uri.getQuery();
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "UrlParser{"
                + "protocol="
                + protocol
                + ", host='"
                + host
                + '\''
                + ", port="
                + port
                + ", path='"
                + path
                + '\''
                + ", query='"
                + query
                + '\''
                + '}';
    }
}

enum Protocol {
    HTTP,
    HTTPS,
    FTP,
    REDIS,
    JDBC,
    PULSAR,
    SMTP;

    public static Protocol from(String protocol) {
        try {
            return Protocol.valueOf(protocol.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException("Unsupported protocol: " + protocol);
        }
    }
}
