package org.example.gersangtrade.hunt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 덱 스냅샷 JSON 직렬화 및 content_hash 생성.
 */
@Component
@RequiredArgsConstructor
public class DeckSnapshotHashUtil {

    private final ObjectMapper objectMapper;

    /** 키 정렬 직렬화 후 SHA-256 hex */
    public String toCanonicalJson(Object content) {
        try {
            ObjectMapper canonical = objectMapper.copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return canonical.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("덱 스냅샷 JSON 직렬화 실패", e);
        }
    }

    public String sha256Hex(String canonicalJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}
