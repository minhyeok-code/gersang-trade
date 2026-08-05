package org.example.gersangtrade.guide.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.Guide;
import org.example.gersangtrade.domain.guide.GuideStep;
import org.example.gersangtrade.guide.dto.request.GuideImport;
import org.example.gersangtrade.guide.dto.request.GuideImportRequest;
import org.example.gersangtrade.guide.dto.request.GuideStepAdminUpdateRequest;
import org.example.gersangtrade.guide.dto.request.GuideStepImport;
import org.example.gersangtrade.guide.dto.request.GuideStepLink;
import org.example.gersangtrade.guide.dto.response.GuideAdminSummaryResponse;
import org.example.gersangtrade.guide.dto.response.GuideImportReport;
import org.example.gersangtrade.guide.dto.response.GuideImportResult;
import org.example.gersangtrade.guide.dto.response.GuideStepIssue;
import org.example.gersangtrade.guide.repository.GuideRepository;
import org.example.gersangtrade.guide.repository.GuideStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가이드 원본 관리 서비스 (관리자).
 * 이미지 추출 JSON 일괄 주입(이름 매칭 + 리포트)과, 검수용 공개/삭제/스텝 수정·삭제를 담당한다.
 * 주입된 가이드는 published=false로 저장되어 검수 후 공개된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuideAdminService {

    private final GuideRepository guideRepository;
    private final GuideStepRepository guideStepRepository;
    private final ItemRepository itemRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final MercenaryRepository mercenaryRepository;

    // ── 일괄 주입 ─────────────────────────────────────────────────────────────────

    /**
     * 가이드 일괄 주입.
     * 스텝의 link.matchName으로 카탈로그를 이름 매칭한다 — 정확히 1건이면 연결,
     * 0건(UNMATCHED)이거나 2건 이상(AMBIGUOUS)이면 링크 없이 저장하고 리포트에 남긴다.
     */
    public GuideImportReport importGuides(GuideImportRequest request) {
        Map<String, Guide> byTitle = new HashMap<>();
        List<GuideImportResult> results = new ArrayList<>();

        // 1차: 가이드·스텝 생성 + 매칭
        for (GuideImport gi : request.guides()) {
            Mercenary target = resolveMercenaryByName(gi.targetMercenaryName());
            Guide guide = Guide.builder()
                    .title(gi.title())
                    .targetMercenary(target)
                    .phase(gi.phase())
                    .version(gi.version())
                    .author(gi.author())
                    .build(); // published=false (기본값) — 검수 후 공개
            guideRepository.save(guide);
            byTitle.put(gi.title(), guide);

            List<GuideStepIssue> issues = new ArrayList<>();
            int linked = 0;

            List<GuideStepImport> steps = gi.steps() != null ? gi.steps() : List.of();
            for (GuideStepImport si : steps) {
                Item linkedItem = null;
                EquipmentSet linkedSet = null;
                Mercenary linkedMercenary = null;

                GuideStepLink link = si.link();
                if (link != null && link.matchName() != null && !link.matchName().isBlank()) {
                    String kind = link.kind() != null ? link.kind().trim().toUpperCase() : "";
                    String name = link.matchName().trim();
                    String despaced = name.replaceAll("\\s+", ""); // 공백 무시 매칭 ("고급 천왕검"↔"고급천왕검")
                    List<?> matches = switch (kind) {
                        case "ITEM" -> itemRepository.findByNameSpaceInsensitive(despaced);
                        case "SET" -> equipmentSetRepository.findByNameSpaceInsensitive(despaced);
                        case "MERCENARY" -> mercenaryRepository.findByNameSpaceInsensitive(despaced);
                        default -> List.of();
                    };
                    if (matches.size() == 1) {
                        Object hit = matches.get(0);
                        if (hit instanceof Item it) linkedItem = it;
                        else if (hit instanceof EquipmentSet es) linkedSet = es;
                        else if (hit instanceof Mercenary mc) linkedMercenary = mc;
                        linked++;
                    } else {
                        // 0건 UNMATCHED, 2건 이상 AMBIGUOUS — 링크 없이 저장하고 리포트
                        issues.add(new GuideStepIssue(si.order(), si.label(), kind, name,
                                matches.isEmpty() ? "UNMATCHED" : "AMBIGUOUS"));
                    }
                }

                GuideStep step = GuideStep.builder()
                        .guide(guide)
                        .stepOrder(si.order())
                        .stepType(si.stepType())
                        .label(si.label())
                        .note(si.note())
                        .linkedItem(linkedItem)
                        .linkedSet(linkedSet)
                        .linkedMercenary(linkedMercenary)
                        .iconUrl(null)
                        .build();
                guideStepRepository.save(step);
            }

            int unmatched = (int) issues.stream().filter(i -> "UNMATCHED".equals(i.status())).count();
            int ambiguous = (int) issues.stream().filter(i -> "AMBIGUOUS".equals(i.status())).count();
            results.add(new GuideImportResult(
                    guide.getId(), gi.title(), target != null,
                    steps.size(), linked, unmatched, ambiguous, issues));
        }

        // 2차: 일반 → 각성 등 nextGuide 연결 (같은 요청 내 제목으로 매칭)
        for (GuideImport gi : request.guides()) {
            if (gi.nextGuideTitle() == null) continue;
            Guide guide = byTitle.get(gi.title());
            Guide next = byTitle.get(gi.nextGuideTitle());
            if (guide != null && next != null) {
                guide.linkNextGuide(next);
            }
        }

        return new GuideImportReport(results.size(), results);
    }

    // ── 검수 (목록/공개/삭제/스텝) ─────────────────────────────────────────────────

    /** 관리자용 전체 가이드 목록 (비공개 포함, 스텝 수·공개여부 포함). */
    @Transactional(readOnly = true)
    public List<GuideAdminSummaryResponse> getAllGuides() {
        return guideRepository.findAllByOrderByIdAsc().stream()
                .map(g -> GuideAdminSummaryResponse.of(g, guideStepRepository.countByGuideId(g.getId())))
                .toList();
    }

    /** 가이드 공개 여부 설정. */
    public void setPublished(Long guideId, boolean published) {
        loadGuide(guideId).updatePublished(published);
    }

    /** 가이드 삭제 — 소속 스텝을 먼저 지우고 가이드를 하드삭제한다. */
    public void deleteGuide(Long guideId) {
        Guide guide = loadGuide(guideId);
        guideStepRepository.deleteByGuideId(guideId);
        guideRepository.delete(guide);
    }

    /** 원본 스텝 검수 수정 — 내용·유형·카탈로그 연결 재설정. */
    public void updateStep(Long stepId, GuideStepAdminUpdateRequest req) {
        GuideStep step = guideStepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스텝을 찾을 수 없습니다: " + stepId));
        step.updateType(req.stepType());
        step.updateContent(req.label(), req.note(), req.iconUrl());
        step.updateLinks(
                resolveItem(req.linkedItemId()),
                resolveSet(req.linkedSetId()),
                resolveMercenary(req.linkedMercenaryId()));
    }

    /** 원본 스텝 삭제. */
    public void deleteStep(Long stepId) {
        GuideStep step = guideStepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스텝을 찾을 수 없습니다: " + stepId));
        guideStepRepository.delete(step);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    private Guide loadGuide(Long guideId) {
        return guideRepository.findById(guideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "가이드를 찾을 수 없습니다: " + guideId));
    }

    /** 이름으로 용병 매칭 (공백 무시) — 없거나 중복이면 null(주입은 계속 진행). */
    private Mercenary resolveMercenaryByName(String name) {
        if (name == null || name.isBlank()) return null;
        List<Mercenary> matches = mercenaryRepository.findByNameSpaceInsensitive(name.replaceAll("\\s+", ""));
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private Item resolveItem(Long id) {
        if (id == null) return null;
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아이템을 찾을 수 없습니다: " + id));
    }

    private EquipmentSet resolveSet(Long id) {
        if (id == null) return null;
        return equipmentSetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트를 찾을 수 없습니다: " + id));
    }

    private Mercenary resolveMercenary(Long id) {
        if (id == null) return null;
        return mercenaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "용병을 찾을 수 없습니다: " + id));
    }
}
