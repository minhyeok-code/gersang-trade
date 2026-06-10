package org.example.gersangtrade.hunt.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.hunt.dto.HuntMonsterSummary;
import org.example.gersangtrade.hunt.dto.response.HuntPublicRecordResponse;
import org.example.gersangtrade.hunt.dto.response.HuntSnapshotResponse;
import org.example.gersangtrade.hunt.service.HuntHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사냥 허브 공개 API.
 * 미해금 사용자는 records·snapshots 조회 시 403.
 */
@RestController
@RequestMapping("/api/hunt")
@RequiredArgsConstructor
public class HuntHubController {

    private final HuntHubService huntHubService;

    /** 몬스터 목록·공개 표본 수 (미해금 허용) */
    @GetMapping("/monsters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HuntMonsterSummary>> getMonsters() {
        return ResponseEntity.ok(huntHubService.getMonsterSummaries());
    }

    /** 몬스터별 공개 클리어타임 랭킹 (해금 필요) */
    @GetMapping("/monsters/{monsterId}/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HuntPublicRecordResponse>> getMonsterRecords(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long monsterId) {
        return ResponseEntity.ok(huntHubService.getPublicRecords(userId, monsterId));
    }

    /** 공개 덱 스냅샷 상세 (해금 필요) */
    @GetMapping("/snapshots/{snapshotId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HuntSnapshotResponse> getSnapshot(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long snapshotId) {
        return ResponseEntity.ok(huntHubService.getPublicSnapshot(userId, snapshotId));
    }
}
