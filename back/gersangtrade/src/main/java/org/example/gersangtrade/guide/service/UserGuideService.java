package org.example.gersangtrade.guide.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.guide.Guide;
import org.example.gersangtrade.domain.guide.GuideStep;
import org.example.gersangtrade.domain.guide.UserGuide;
import org.example.gersangtrade.domain.guide.UserGuideStep;
import org.example.gersangtrade.domain.guide.enums.GuideStepType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.guide.dto.response.UserGuideDetailResponse;
import org.example.gersangtrade.guide.dto.response.UserGuideStepResponse;
import org.example.gersangtrade.guide.dto.response.UserGuideSummaryResponse;
import org.example.gersangtrade.guide.repository.GuideRepository;
import org.example.gersangtrade.guide.repository.GuideStepRepository;
import org.example.gersangtrade.guide.repository.UserGuideRepository;
import org.example.gersangtrade.guide.repository.UserGuideStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 유저 가이드 사본 서비스.
 * 원본 복제(clone-on-start), 진행도 체크, 스텝 추가/수정/삭제/순서변경, 이름변경, 소프트삭제를 담당한다.
 * 모든 변경은 소유자 검증(해당 유저의 사본인지)을 거친다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserGuideService {

    private final UserGuideRepository userGuideRepository;
    private final UserGuideStepRepository userGuideStepRepository;
    private final GuideRepository guideRepository;
    private final GuideStepRepository guideStepRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;
    private final MercenaryRepository mercenaryRepository;

    // ── 사본 생성/조회 ────────────────────────────────────────────────────────────

    /**
     * 원본 가이드를 복제해 유저 사본을 생성한다 (clone-on-start).
     * 같은 원본에 대한 활성 사본이 이미 있으면 그것을 그대로 반환한다(멱등).
     */
    public UserGuideDetailResponse startGuide(Long userId, Long guideId) {
        // 활성 사본 유일성 — 앱 레벨 강제. 이미 있으면 그 상세를 반환(멱등).
        UserGuide existing = userGuideRepository
                .findByUser_IdAndSourceGuide_IdAndDeletedAtIsNull(userId, guideId)
                .orElse(null);
        if (existing != null) {
            List<UserGuideStep> existingSteps =
                    userGuideStepRepository.findByUserGuideIdOrderByStepOrderAsc(existing.getId());
            return UserGuideDetailResponse.of(existing, existingSteps, buildSetIcons(existingSteps));
        }

        Guide guide = guideRepository.findByIdAndPublishedTrue(guideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "가이드를 찾을 수 없습니다: " + guideId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + userId));

        UserGuide userGuide = UserGuide.builder()
                .user(user)
                .sourceGuide(guide)
                .sourceVersion(guide.getVersion())
                .title(guide.getTitle())
                .targetMercenary(guide.getTargetMercenary())
                .build();
        userGuideRepository.save(userGuide);

        // 원본 스텝을 순번대로 복제 (custom=false)
        List<GuideStep> originSteps = guideStepRepository.findByGuideIdOrderByStepOrderAsc(guideId);
        List<UserGuideStep> copies = originSteps.stream()
                .map(origin -> UserGuideStep.builder()
                        .userGuide(userGuide)
                        .stepOrder(origin.getStepOrder())
                        .stepType(origin.getStepType())
                        .label(origin.getLabel())
                        .note(origin.getNote())
                        .linkedItem(origin.getLinkedItem())
                        .linkedSet(origin.getLinkedSet())
                        .linkedMercenary(origin.getLinkedMercenary())
                        .iconUrl(origin.getIconUrl())
                        .custom(false)
                        .build())
                .toList();
        userGuideStepRepository.saveAll(copies);

        return UserGuideDetailResponse.of(userGuide, copies, buildSetIcons(copies));
    }

    /** 유저의 활성 사본 목록 (최신순, 진행도 포함). */
    @Transactional(readOnly = true)
    public List<UserGuideSummaryResponse> getMyGuides(Long userId) {
        return userGuideRepository.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(g -> UserGuideSummaryResponse.of(g,
                        userGuideStepRepository.countByUserGuideId(g.getId()),
                        userGuideStepRepository.countByUserGuideIdAndCheckedAtIsNotNull(g.getId())))
                .toList();
    }

    /** 사본 상세(스텝 포함) — 소유자 검증 후 반환. */
    @Transactional(readOnly = true)
    public UserGuideDetailResponse getMyGuideDetail(Long userId, Long userGuideId) {
        UserGuide userGuide = loadOwnedGuide(userId, userGuideId);
        List<UserGuideStep> steps = userGuideStepRepository.findByUserGuideIdOrderByStepOrderAsc(userGuideId);
        return UserGuideDetailResponse.of(userGuide, steps, buildSetIcons(steps));
    }

    // ── 진행도 ───────────────────────────────────────────────────────────────────

    /** 스텝 완료 체크. */
    public void checkStep(Long userId, Long stepId) {
        loadOwnedStep(userId, stepId).check();
    }

    /** 스텝 완료 해제. */
    public void uncheckStep(Long userId, Long stepId) {
        loadOwnedStep(userId, stepId).uncheck();
    }

    // ── 스텝 편집 ────────────────────────────────────────────────────────────────

    /**
     * 사본에 커스텀 스텝을 추가한다 (맨 뒤에 붙는다, custom=true).
     * 연결 id는 선택 — 주어지면 카탈로그에서 조회해 매물 funnel·이미지에 사용한다.
     */
    public UserGuideStepResponse addStep(Long userId, Long userGuideId,
                                         GuideStepType stepType, String label, String note, String iconUrl,
                                         Long linkedItemId, Long linkedSetId, Long linkedMercenaryId) {
        UserGuide userGuide = loadOwnedGuide(userId, userGuideId);
        if (stepType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스텝 유형은 필수입니다.");
        }
        if (label == null || label.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스텝 이름은 필수입니다.");
        }

        int nextOrder = userGuideStepRepository.findMaxStepOrder(userGuideId) + 1;
        UserGuideStep step = UserGuideStep.builder()
                .userGuide(userGuide)
                .stepOrder(nextOrder)
                .stepType(stepType)
                .label(label.trim())
                .note(note)
                .linkedItem(resolveItem(linkedItemId))
                .linkedSet(resolveSet(linkedSetId))
                .linkedMercenary(resolveMercenary(linkedMercenaryId))
                .iconUrl(iconUrl)
                .custom(true)
                .build();
        userGuideStepRepository.save(step);
        return UserGuideStepResponse.of(step, buildSetIcons(List.of(step))
                .getOrDefault(step.getLinkedSet() != null ? step.getLinkedSet().getId() : -1L, List.of()));
    }

    /** 스텝 표시 내용 수정. */
    public void updateStepContent(Long userId, Long stepId, String label, String note, String iconUrl) {
        loadOwnedStep(userId, stepId).updateContent(label, note, iconUrl);
    }

    /** 스텝 삭제 (개인 데이터라 하드삭제). */
    public void removeStep(Long userId, Long stepId) {
        UserGuideStep step = loadOwnedStep(userId, stepId);
        userGuideStepRepository.delete(step);
    }

    /**
     * 스텝 순서 재정렬.
     * orderedStepIds는 해당 사본의 전체 스텝 id를 원하는 순서대로 나열한 것이어야 한다.
     */
    public void reorderSteps(Long userId, Long userGuideId, List<Long> orderedStepIds) {
        loadOwnedGuide(userId, userGuideId);
        List<UserGuideStep> steps = userGuideStepRepository.findByUserGuideIdOrderByStepOrderAsc(userGuideId);

        // 요청 id 집합이 사본의 전체 스텝과 정확히 일치하는지 검증
        if (orderedStepIds == null || orderedStepIds.size() != steps.size()
                || !orderedStepIds.stream().collect(java.util.stream.Collectors.toSet())
                        .equals(steps.stream().map(UserGuideStep::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "순서 목록은 사본의 전체 스텝과 일치해야 합니다.");
        }

        java.util.Map<Long, UserGuideStep> byId = steps.stream()
                .collect(java.util.stream.Collectors.toMap(UserGuideStep::getId, s -> s));
        for (int i = 0; i < orderedStepIds.size(); i++) {
            byId.get(orderedStepIds.get(i)).updateOrder(i + 1);
        }
    }

    // ── 사본 관리 ────────────────────────────────────────────────────────────────

    /** 사본 이름 변경. */
    public void renameGuide(Long userId, Long userGuideId, String title) {
        loadOwnedGuide(userId, userGuideId).updateTitle(title);
    }

    /** 사본 소프트삭제. */
    public void deleteGuide(Long userId, Long userGuideId) {
        loadOwnedGuide(userId, userGuideId).softDelete();
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    /** 스텝들의 연결 세트별 피스 이미지 목록(최대 4)을 만든다 — 프론트 세트 콜라주용. */
    private Map<Long, List<String>> buildSetIcons(List<UserGuideStep> steps) {
        List<Long> setIds = steps.stream()
                .map(UserGuideStep::getLinkedSet)
                .filter(x -> x != null)
                .map(EquipmentSet::getId)
                .distinct()
                .toList();
        Map<Long, List<String>> map = new HashMap<>();
        for (Long setId : setIds) {
            List<String> icons = equipmentSetPieceRepository.findWithItemByEquipmentSetId(setId).stream()
                    .map(p -> p.getEquipmentItem().getItem().getImageUrl())
                    .filter(url -> url != null && !url.isBlank())
                    .limit(5)
                    .toList();
            map.put(setId, icons);
        }
        return map;
    }

    /** 활성 사본을 소유자 검증과 함께 로드한다. */
    private UserGuide loadOwnedGuide(Long userId, Long userGuideId) {
        UserGuide userGuide = userGuideRepository.findByIdAndDeletedAtIsNull(userGuideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "가이드 사본을 찾을 수 없습니다: " + userGuideId));
        if (!userGuide.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 가이드만 접근할 수 있습니다.");
        }
        return userGuide;
    }

    /** 스텝을 소유자 검증과 함께 로드한다 (사본이 소프트삭제되지 않았는지도 확인). */
    private UserGuideStep loadOwnedStep(Long userId, Long stepId) {
        UserGuideStep step = userGuideStepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "스텝을 찾을 수 없습니다: " + stepId));
        UserGuide userGuide = step.getUserGuide();
        if (userGuide.getDeletedAt() != null || !userGuide.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 가이드만 접근할 수 있습니다.");
        }
        return step;
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
