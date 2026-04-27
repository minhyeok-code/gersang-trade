package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.CharacteristicCreateRequest;
import org.example.gersangtrade.admin.dto.request.CharacteristicLevelSaveRequest;
import org.example.gersangtrade.admin.dto.request.CharacteristicUpdateRequest;
import org.example.gersangtrade.admin.dto.request.MercenaryStatReplaceRequest;
import org.example.gersangtrade.admin.dto.request.MercenaryUpdateRequest;
import org.example.gersangtrade.admin.dto.request.SkillReplaceRequest;
import org.example.gersangtrade.admin.dto.response.CharacteristicAdminResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryAdminResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryDetailAdminResponse;
import org.example.gersangtrade.admin.service.MercenaryAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 용병 특성 수동 관리 API.
 *
 * <p>거상짱에서 제공하지 않는 특성 레벨별 수치를 관리자가 직접 입력한다.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>GET  /admin/mercenaries                              — 용병 목록 (특성 수 포함)</li>
 *   <li>GET  /admin/mercenaries/{id}/characteristics         — 특성 목록 (레벨 수치 포함)</li>
 *   <li>POST /admin/mercenaries/{id}/characteristics         — 특성 추가</li>
 *   <li>PUT  /admin/mercenaries/{id}/characteristics/{charId} — 특성 수정</li>
 *   <li>DELETE /admin/mercenaries/{id}/characteristics/{charId} — 특성 삭제</li>
 *   <li>PUT  /admin/mercenaries/{id}/characteristics/{charId}/levels — 레벨 수치 일괄 저장</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/mercenaries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MercenaryAdminController {

    private final MercenaryAdminService mercenaryAdminService;

    /** 용병 단건 상세 조회 (기본정보 + 스탯 + 스킬). */
    @GetMapping("/{mercenaryId}")
    public ResponseEntity<MercenaryDetailAdminResponse> getMercenary(
            @PathVariable Long mercenaryId) {
        return ResponseEntity.ok(mercenaryAdminService.getMercenary(mercenaryId));
    }

    /** 용병 기본정보 수정 — 이름·카테고리·국가·속성·속성값·출시예정. */
    @PutMapping("/{mercenaryId}")
    public ResponseEntity<MercenaryDetailAdminResponse> updateInfo(
            @PathVariable Long mercenaryId,
            @Valid @RequestBody MercenaryUpdateRequest request) {
        return ResponseEntity.ok(mercenaryAdminService.updateInfo(mercenaryId, request));
    }

    /** 용병 스탯 전체 교체 (PUT 의미론 — 기존 전부 삭제 후 재적재). */
    @PutMapping("/{mercenaryId}/stats")
    public ResponseEntity<MercenaryDetailAdminResponse> replaceStats(
            @PathVariable Long mercenaryId,
            @Valid @RequestBody MercenaryStatReplaceRequest request) {
        return ResponseEntity.ok(mercenaryAdminService.replaceStats(mercenaryId, request));
    }

    /** 용병 스킬 전체 교체 (PUT 의미론 — 기존 전부 삭제 후 재적재). */
    @PutMapping("/{mercenaryId}/skills")
    public ResponseEntity<MercenaryDetailAdminResponse> replaceSkills(
            @PathVariable Long mercenaryId,
            @Valid @RequestBody SkillReplaceRequest request) {
        return ResponseEntity.ok(mercenaryAdminService.replaceSkills(mercenaryId, request));
    }

    /**
     * 용병 목록 조회.
     * characteristicCount로 특성 미입력 용병을 빠르게 파악할 수 있다.
     */
    @GetMapping
    public ResponseEntity<Page<MercenaryAdminResponse>> listMercenaries(
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(mercenaryAdminService.listMercenaries(pageable));
    }

    /**
     * 용병 특성 목록 조회 (레벨 수치 포함).
     * requiredCharacteristicId null 여부로 루트/자식 특성을 구분한다.
     */
    @GetMapping("/{mercenaryId}/characteristics")
    public ResponseEntity<List<CharacteristicAdminResponse>> listCharacteristics(
            @PathVariable Long mercenaryId) {
        return ResponseEntity.ok(mercenaryAdminService.listCharacteristics(mercenaryId));
    }

    /** 특성 추가 (최대 4개). 선행 특성이 있으면 requiredCharacteristicId를 지정한다. */
    @PostMapping("/{mercenaryId}/characteristics")
    public ResponseEntity<CharacteristicAdminResponse> createCharacteristic(
            @PathVariable Long mercenaryId,
            @Valid @RequestBody CharacteristicCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mercenaryAdminService.createCharacteristic(mercenaryId, request));
    }

    /** 특성 수정 — 이름·포인트·설명·선행 특성 변경. 레벨 수치는 별도 엔드포인트 사용. */
    @PutMapping("/{mercenaryId}/characteristics/{charId}")
    public ResponseEntity<CharacteristicAdminResponse> updateCharacteristic(
            @PathVariable Long mercenaryId,
            @PathVariable Long charId,
            @Valid @RequestBody CharacteristicUpdateRequest request) {
        return ResponseEntity.ok(
                mercenaryAdminService.updateCharacteristic(mercenaryId, charId, request));
    }

    /** 특성 삭제. 자식 특성이 있으면 삭제 거부 (400). */
    @DeleteMapping("/{mercenaryId}/characteristics/{charId}")
    public ResponseEntity<Void> deleteCharacteristic(
            @PathVariable Long mercenaryId,
            @PathVariable Long charId) {
        mercenaryAdminService.deleteCharacteristic(mercenaryId, charId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 레벨 수치 일괄 저장 (PUT 의미론).
     * 기존 레벨을 전부 삭제하고 요청 목록으로 재적재한다.
     * 각성 특성(레벨 없음)은 빈 배열을 전송하면 된다.
     */
    @PutMapping("/{mercenaryId}/characteristics/{charId}/levels")
    public ResponseEntity<CharacteristicAdminResponse> saveLevels(
            @PathVariable Long mercenaryId,
            @PathVariable Long charId,
            @Valid @RequestBody CharacteristicLevelSaveRequest request) {
        return ResponseEntity.ok(
                mercenaryAdminService.saveLevels(mercenaryId, charId, request));
    }
}
