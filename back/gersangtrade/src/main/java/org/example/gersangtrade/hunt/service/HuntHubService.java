package org.example.gersangtrade.hunt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.hunt.ClearTimeRecordStatus;
import org.example.gersangtrade.domain.hunt.DeckSnapshot;
import org.example.gersangtrade.domain.user.UserClearTimeRepository;
import org.example.gersangtrade.hunt.dto.HuntMonsterSummary;
import org.example.gersangtrade.hunt.dto.response.HuntHubStatusResponse;
import org.example.gersangtrade.hunt.dto.response.HuntPublicRecordResponse;
import org.example.gersangtrade.hunt.dto.response.HuntSnapshotResponse;
import org.example.gersangtrade.hunt.dto.response.MyClearTimeResponse;
import org.example.gersangtrade.hunt.repository.DeckSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 사냥 허브 조회 서비스 — 내 기록, 해금 상태, 공개 랭킹·스냅샷.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HuntHubService {

    private final UserClearTimeRepository clearTimeRepository;
    private final DeckSnapshotRepository deckSnapshotRepository;
    private final HuntHubAccessService accessService;
    private final ObjectMapper objectMapper;

    public List<MyClearTimeResponse> getMyClearTimes(Long userId) {
        return clearTimeRepository.findByUserIdWithDetailsOrderByRecordedAtDesc(userId).stream()
                .map(MyClearTimeResponse::from)
                .toList();
    }

    public HuntHubStatusResponse getHubStatus(Long userId) {
        int distinct = accessService.countDistinctMonstersForUnlock(userId);
        int required = accessService.requiredDistinctMonsters();
        return new HuntHubStatusResponse(distinct, required, distinct >= required);
    }

    /** 소프트 게이트 — 미해금도 몬스터 목록·표본 수 조회 가능 */
    public List<HuntMonsterSummary> getMonsterSummaries() {
        return clearTimeRepository.summarizePublicRecordsByMonster(ClearTimeRecordStatus.ACTIVE);
    }

    public List<HuntPublicRecordResponse> getPublicRecords(Long userId, Long monsterId) {
        accessService.requireUnlocked(userId);
        return clearTimeRepository.findPublicByMonsterId(monsterId, ClearTimeRecordStatus.ACTIVE).stream()
                .map(HuntPublicRecordResponse::from)
                .toList();
    }

    public HuntSnapshotResponse getPublicSnapshot(Long userId, Long snapshotId) {
        accessService.requireUnlocked(userId);

        if (!clearTimeRepository.existsByDeckSnapshot_IdAndIsPublicTrueAndStatus(
                snapshotId, ClearTimeRecordStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공개된 스냅샷을 찾을 수 없습니다.");
        }

        DeckSnapshot snapshot = deckSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NoSuchElementException("스냅샷을 찾을 수 없습니다."));

        JsonNode content;
        try {
            content = objectMapper.readTree(snapshot.getContentJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스냅샷 JSON 파싱 실패: " + snapshotId, e);
        }
        return HuntSnapshotResponse.of(snapshot, content);
    }
}
