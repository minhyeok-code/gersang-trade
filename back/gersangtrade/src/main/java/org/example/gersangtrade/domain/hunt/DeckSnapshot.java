package org.example.gersangtrade.domain.hunt;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클리어타임 저장 시점의 덱 구성 불변 스냅샷.
 */
@Entity
@Table(
        name = "deck_snapshots",
        indexes = @Index(name = "uq_deck_snapshots_content_hash", columnList = "content_hash", unique = true)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DeckSnapshot(String contentJson, String contentHash) {
        this.contentJson = contentJson;
        this.contentHash = contentHash;
        this.createdAt = LocalDateTime.now();
    }
}
