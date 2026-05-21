package org.example.gersangtrade.deck.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.deck.dto.request.*;
import org.example.gersangtrade.deck.dto.response.*;
import org.example.gersangtrade.deck.service.DeckService;
import org.example.gersangtrade.domain.deck.enums.EquipSlot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 덱 관리 API 컨트롤러.
 *
 * <pre>
 * GET    /api/decks                                                         — 내 덱 목록
 * POST   /api/decks                                                         — 덱 생성
 * GET    /api/decks/{deckId}                                                — 덱 상세
 * PATCH  /api/decks/{deckId}                                                — 덱 이름·활성화 수정
 * DELETE /api/decks/{deckId}                                                — 덱 삭제
 * POST   /api/decks/{deckId}/members                                        — 용병 추가
 * DELETE /api/decks/{deckId}/members/{memberId}                             — 용병 제거
 * GET    /api/decks/{deckId}/members/{memberId}/stats                       — 용병 합산 스탯 조회
 * PUT    /api/decks/{deckId}/members/{memberId}/slots/{slot}                — 장비 착용
 * DELETE /api/decks/{deckId}/members/{memberId}/slots/{slot}                — 장비 해제
 * PUT    /api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual         — 주술 등록
 * DELETE /api/decks/{deckId}/members/{memberId}/slots/{slot}/ritual         — 주술 제거
 * </pre>
 */
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeckController {

    private final DeckService deckService;

    // ── 덱 CRUD ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<DeckSummaryResponse>> getMyDecks(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(deckService.getMyDecks(userId));
    }

    @PostMapping
    public ResponseEntity<DeckSummaryResponse> createDeck(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid DeckCreateRequest req) {
        DeckSummaryResponse created = deckService.createDeck(userId, req);
        return ResponseEntity.created(URI.create("/api/decks/" + created.id())).body(created);
    }

    @GetMapping("/{deckId}")
    public ResponseEntity<DeckDetailResponse> getDeckDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId) {
        return ResponseEntity.ok(deckService.getDeckDetail(userId, deckId));
    }

    @PatchMapping("/{deckId}")
    public ResponseEntity<Void> updateDeck(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @RequestBody @Valid DeckUpdateRequest req) {
        deckService.updateDeck(userId, deckId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId) {
        deckService.deleteDeck(userId, deckId);
        return ResponseEntity.noContent().build();
    }

    // ── 용병 ─────────────────────────────────────────────────────────────────

    @PostMapping("/{deckId}/members")
    public ResponseEntity<DeckMemberResponse> addMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @RequestBody @Valid DeckMemberAddRequest req) {
        DeckMemberResponse added = deckService.addMember(userId, deckId, req);
        return ResponseEntity.created(URI.create("/api/decks/" + deckId + "/members/" + added.id())).body(added);
    }

    @DeleteMapping("/{deckId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId) {
        deckService.removeMember(userId, deckId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{deckId}/members/{memberId}/stats")
    public ResponseEntity<MemberStatResponse> getMemberStats(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(deckService.getMemberStats(userId, deckId, memberId));
    }

    // ── 장비 슬롯 ────────────────────────────────────────────────────────────

    @PutMapping("/{deckId}/members/{memberId}/slots/{slot}")
    public ResponseEntity<Void> equipSlot(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId,
            @PathVariable EquipSlot slot,
            @RequestBody @Valid SlotEquipRequest req) {
        deckService.equipSlot(userId, deckId, memberId, slot, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{deckId}/members/{memberId}/slots/{slot}")
    public ResponseEntity<Void> unequipSlot(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId,
            @PathVariable EquipSlot slot) {
        deckService.unequipSlot(userId, deckId, memberId, slot);
        return ResponseEntity.noContent().build();
    }

    // ── 주술 ─────────────────────────────────────────────────────────────────

    @PutMapping("/{deckId}/members/{memberId}/slots/{slot}/ritual")
    public ResponseEntity<Void> applyRitual(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId,
            @PathVariable EquipSlot slot,
            @RequestBody @Valid SlotRitualRequest req) {
        deckService.applyRitual(userId, deckId, memberId, slot, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{deckId}/members/{memberId}/slots/{slot}/ritual")
    public ResponseEntity<Void> removeRitual(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deckId,
            @PathVariable Long memberId,
            @PathVariable EquipSlot slot) {
        deckService.removeRitual(userId, deckId, memberId, slot);
        return ResponseEntity.noContent().build();
    }
}
