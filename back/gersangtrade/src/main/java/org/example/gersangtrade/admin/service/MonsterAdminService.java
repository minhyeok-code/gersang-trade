package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.monster.MonsterAdminResponse;
import org.example.gersangtrade.admin.dto.monster.MonsterCreateRequest;
import org.example.gersangtrade.admin.dto.monster.MonsterUpdateRequest;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.domain.catalog.Monster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 몬스터 관리자 서비스 — CRUD */
@Service
@RequiredArgsConstructor
public class MonsterAdminService {

    private final MonsterRepository monsterRepository;

    // ── 목록 조회 ────────────────────────────────────────────────────────────

    /**
     * 몬스터 목록 페이징 조회.
     * name 파라미터가 있으면 이름 부분 검색을 적용한다.
     */
    @Transactional(readOnly = true)
    public Page<MonsterAdminResponse> list(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return monsterRepository.searchByName(name, pageable)
                    .map(MonsterAdminResponse::from);
        }
        return monsterRepository.findAll(pageable).map(MonsterAdminResponse::from);
    }

    // ── 단건 조회 ────────────────────────────────────────────────────────────

    /** 몬스터 단건 조회 */
    @Transactional(readOnly = true)
    public MonsterAdminResponse get(Long id) {
        return MonsterAdminResponse.from(findOrThrow(id));
    }

    // ── 신규 등록 ────────────────────────────────────────────────────────────

    /**
     * 몬스터를 신규 등록한다.
     * 동일 이름이 이미 존재하면 409를 반환한다.
     */
    @Transactional
    public MonsterAdminResponse create(MonsterCreateRequest req) {
        if (monsterRepository.findByName(req.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 동일한 이름의 몬스터가 존재합니다: " + req.name());
        }
        Monster monster = monsterRepository.save(Monster.builder()
                .name(req.name())
                .hp(req.hp())
                .hittingResistance(req.hittingResistance())
                .magicResistance(req.magicResistance())
                .elementValue(req.elementValue())
                .element(req.element())
                .build());
        return MonsterAdminResponse.from(monster);
    }

    // ── 수정 ─────────────────────────────────────────────────────────────────

    /**
     * 몬스터 정보를 수정한다.
     * null인 필드는 기존 값을 유지한다.
     */
    @Transactional
    public MonsterAdminResponse update(Long id, MonsterUpdateRequest req) {
        Monster monster = findOrThrow(id);

        // null이 아닌 필드만 갱신 — Monster.update()는 전체 교체이므로 기존 값으로 대체
        Long hp = req.hp() != null ? req.hp() : monster.getHp();
        Integer hitting = req.hittingResistance() != null
                ? req.hittingResistance() : monster.getHittingResistance();
        Integer magic = req.magicResistance() != null
                ? req.magicResistance() : monster.getMagicResistance();
        Integer elemVal = req.elementValue() != null
                ? req.elementValue() : monster.getElementValue();
        var element = req.element() != null ? req.element() : monster.getElement();

        monster.update(hp, hitting, magic, elemVal, element);

        // 이름 변경 요청이 있으면 별도 처리 (Monster에 updateName이 없으므로 직접 재저장)
        if (req.name() != null && !req.name().isBlank()) {
            // 이름 변경 시 중복 확인 (자기 자신 제외)
            monsterRepository.findByName(req.name())
                    .filter(m -> !m.getId().equals(id))
                    .ifPresent(m -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "이미 동일한 이름의 몬스터가 존재합니다: " + req.name());
                    });
            monster.updateName(req.name());
        }

        return MonsterAdminResponse.from(monster);
    }

    // ── 숨김 여부 토글 ────────────────────────────────────────────────────────

    /**
     * 몬스터 숨김 여부를 수동으로 설정한다.
     * 자동 규칙(속성값 없음, 明속성)과 관계없이 관리자가 직접 제어할 수 있다.
     */
    @Transactional
    public MonsterAdminResponse setHidden(Long id, boolean hidden) {
        Monster monster = findOrThrow(id);
        monster.updateHidden(hidden);
        return MonsterAdminResponse.from(monster);
    }

    // ── 삭제 ─────────────────────────────────────────────────────────────────

    /** 몬스터 삭제 */
    @Transactional
    public void delete(Long id) {
        Monster monster = findOrThrow(id);
        monsterRepository.delete(monster);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private Monster findOrThrow(Long id) {
        return monsterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "몬스터를 찾을 수 없습니다: " + id));
    }
}
