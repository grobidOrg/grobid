package org.grobid.core.utilities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hash a string using sha1.
 *
 */
public class SHA1 {

    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SHA1.class);

    /**
     * Error message.
     */
    private static final String ERROR_WHILE_EXECUTING_SHA1 = "Error while executing sha1:";

    /**
     * Return the hash value of argument using SHA1 algorithm.
     *
     * @param pArg
     * the value to hash.
     * @return The hashed value.
     */
    public static String getSHA1(String pArg) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(pArg.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException exp) {
            LOGGER.error(ERROR_WHILE_EXECUTING_SHA1 + exp);
        } catch (UnsupportedEncodingException exp) {
            LOGGER.error(ERROR_WHILE_EXECUTING_SHA1 + exp);
        }
        return sha1;
    }

    /**
     * Convert from byte to hexa.
     * @param hash
     * the input in bytes.
     * @return String
     */
    protected static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
