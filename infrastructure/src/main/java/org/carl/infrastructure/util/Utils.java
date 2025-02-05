package org.carl.infrastructure.util;

import jakarta.xml.bind.DatatypeConverter;
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
}
