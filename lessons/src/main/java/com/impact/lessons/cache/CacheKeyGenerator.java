package com.impact.lessons.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public interface CacheKeyGenerator {
    String generate(String prefix, Object[] args);

    static CacheKeyGenerator defaultGenerator() {
        return new DefaultCacheKeyGenerator();
    }

    final class DefaultCacheKeyGenerator implements CacheKeyGenerator {
        @Override
        public String generate(String prefix, Object[] args) {
            if (args == null || args.length == 0) {
                return prefix + ":static";
            }
            StringBuilder sb = new StringBuilder(prefix);
            for (Object arg : args) {
                sb.append(':').append(normalize(arg));
            }
            String key = sb.toString();
            if (key.length() <= 256) {
                return key;
            }
            return prefix + ":sha256:" + sha256Hex(key);
        }

        private static String normalize(Object arg) {
            if (arg == null) return "null";
            String s = String.valueOf(arg);
            s = s.replaceAll("\\s+", " ").trim();
            return s.length() > 80 ? s.substring(0, 80) : s;
        }

        private static String sha256Hex(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(hashed);
            } catch (Exception e) {
                // Fallback: still deterministic-ish, but should never happen on modern JDKs.
                return Integer.toHexString(input.hashCode());
            }
        }
    }
}

