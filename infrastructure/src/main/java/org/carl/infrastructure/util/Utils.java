package org.carl.infrastructure.util;

import jakarta.xml.bind.DatatypeConverter;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import org.jboss.logging.Logger;

public class Utils {
    private static final Logger log = Logger.getLogger(Utils.class);

    public static String hashMD5(String source) {
        String res = null;
        try {
            var messageDigest = MessageDigest.getInstance("MD5");
            var mdBytes = messageDigest.digest(source.getBytes());
            res = DatatypeConverter.printHexBinary(mdBytes);
        } catch (Exception e) {
            log.error("", e);
        }
        return res;
    }

    // NOTE: URL construct is `Deprecated`
    public static UrlParser createURL(String url) throws URISyntaxException {
        return new UrlParser(url);
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
