package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.set.AdminSetResponse;
import org.example.gersangtrade.admin.dto.set.AdminSetUpdateRequest;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminEquipmentSetService {

    private final EquipmentSetRepository equipmentSetRepository;

    /** 세트 목록 — 이름 검색 + 페이징 */
    @Transactional(readOnly = true)
    public Page<AdminSetResponse> getSets(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return equipmentSetRepository.findByNameContaining(name, pageable)
                    .map(AdminSetResponse::from);
        }
        return equipmentSetRepository.findAll(pageable).map(AdminSetResponse::from);
    }

    /** 세트 단건 조회 */
    @Transactional(readOnly = true)
    public AdminSetResponse getSet(Long id) {
        return AdminSetResponse.from(findOrThrow(id));
    }

    /** 세트 수정 (이름, 피스 수, 거래 노출 여부) */
    @Transactional
    public AdminSetResponse updateSet(Long id, AdminSetUpdateRequest req) {
        EquipmentSet set = findOrThrow(id);
        set.updateInfo(req.name(), req.totalPieces(), req.isTradeable());
        return AdminSetResponse.from(set);
    }

    private EquipmentSet findOrThrow(Long id) {
        return equipmentSetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트를 찾을 수 없습니다. id=" + id));
    }
}
