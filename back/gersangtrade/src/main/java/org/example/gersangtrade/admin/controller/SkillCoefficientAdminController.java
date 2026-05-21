package org.example.gersangtrade.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientCreateRequest;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientJsonRow;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientMeasurementRequest;
import org.example.gersangtrade.admin.dto.request.SkillCoefficientUpdateRequest;
import org.example.gersangtrade.admin.dto.response.SkillCoefficientAdminResponse;
import org.example.gersangtrade.admin.dto.response.SkillCoefficientIssueListResponse;
import org.example.gersangtrade.admin.service.SkillCoefficientAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 스킬 계수 관리 Admin API.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>GET   /admin/skill-coefficients                   — 목록 조회 (?unmeasured=true로 미측정 필터)</li>
 *   <li>GET   /admin/skill-coefficients/issues            — 이슈 항목 목록 (미측정·계수 0·타입 미설정 등)</li>
 *   <li>POST  /admin/skill-coefficients                   — 단건 수동 생성 (JSON 파일에 없는 스킬용)</li>
 *   <li>PUT   /admin/skill-coefficients                   — Skill-coeff.json 배열 bulk upsert</li>
 *   <li>PUT   /admin/skill-coefficients/{id}              — 단건 전체 수정</li>
 *   <li>PATCH /admin/skill-coefficients/{id}/measurement  — casts_per_second / tick_interval_ms 입력</li>
 * </ul>
 *
 * <p>curl 예시:
 * <pre>
 * # JSON 파일 전체 적재
 * curl -X PUT http://localhost:8080/admin/skill-coefficients \
 *   -H "Content-Type: application/json" \
 *   -b "SESSION=<세션쿠키>" \
 *   -d @docs/Skill-coeff.json
 *
 * # 측정값 입력 (INSTANT)
 * curl -X PATCH http://localhost:8080/admin/skill-coefficients/{id}/measurement \
 *   -H "Content-Type: application/json" \
 *   -b "SESSION=<세션쿠키>" \
 *   -d '{"castsPerSecond": 0.45, "measurementNote": "자동전투 기준 측정"}'
 *
 * # 미측정 목록 조회
 * curl http://localhost:8080/admin/skill-coefficients?unmeasured=true \
 *   -b "SESSION=<세션쿠키>"
 * </pre>
 */
@RestController
@RequestMapping("/admin/skill-coefficients")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SkillCoefficientAdminController {

    private final SkillCoefficientAdminService skillCoefficientAdminService;

    /**
     * 스킬 계수 목록 조회.
     * unmeasured=true이면 casts_per_second / tick_interval_ms 미측정 행만 반환한다.
     */
    @GetMapping
    public ResponseEntity<List<SkillCoefficientAdminResponse>> list(
            @RequestParam(defaultValue = "false") boolean unmeasured) {
        return ResponseEntity.ok(skillCoefficientAdminService.list(unmeasured));
    }

    /**
     * 스킬 계수 이슈 목록 조회.
     * 전체 계수를 검사해 문제가 있는 항목만 반환한다.
     * 이슈 종류: MISSING_SKILL_TYPE / UNMEASURED / ALL_COEFS_ZERO / HIT_COUNT_ZERO
     */
    @GetMapping("/issues")
    public ResponseEntity<SkillCoefficientIssueListResponse> listIssues() {
        return ResponseEntity.ok(skillCoefficientAdminService.listIssues());
    }

    /**
     * Skill-coeff.json 배열 bulk upsert.
     * row_id 기준 upsert — 없으면 신규, 있으면 전체 필드 업데이트.
     * 요청 body는 JSON 파일 내용 그대로 전달한다.
     */
    @PutMapping
    public ResponseEntity<Map<String, Integer>> bulkUpsert(
            @Valid @RequestBody List<SkillCoefficientJsonRow> rows) {
        return ResponseEntity.ok(skillCoefficientAdminService.bulkUpsert(rows));
    }

    /**
     * 스킬 계수 수동 단건 생성.
     * mercenarySkillId 또는 itemSkillId 중 하나만 지정한다.
     * JSON 파일에 없는 스킬을 직접 입력할 때 사용한다.
     */
    @PostMapping
    public ResponseEntity<SkillCoefficientAdminResponse> create(
            @Valid @RequestBody SkillCoefficientCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillCoefficientAdminService.create(request));
    }

    /**
     * 스킬 계수 전체 수정 (PUT 의미론).
     * FK(스킬 대상)는 변경 불가. 계수·측정값·메타데이터를 모두 교체한다.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SkillCoefficientAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SkillCoefficientUpdateRequest request) {
        return ResponseEntity.ok(skillCoefficientAdminService.update(id, request));
    }

    /**
     * 직접 측정값 입력.
     * INSTANT   → castsPerSecond 필수.
     * PERSISTENT → tickIntervalMs 필수.
     */
    @PatchMapping("/{id}/measurement")
    public ResponseEntity<SkillCoefficientAdminResponse> updateMeasurement(
            @PathVariable Long id,
            @RequestBody SkillCoefficientMeasurementRequest request) {
        return ResponseEntity.ok(skillCoefficientAdminService.updateMeasurement(id, request));
    }
}
