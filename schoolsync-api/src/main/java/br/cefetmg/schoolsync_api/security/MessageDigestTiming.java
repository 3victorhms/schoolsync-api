package br.cefetmg.schoolsync_api.security;

import java.security.MessageDigest;

final class MessageDigestTiming {

    private MessageDigestTiming() {
    }

    static boolean equals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
