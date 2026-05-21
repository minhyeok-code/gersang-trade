package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.SetResponse;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 유저용 장비 세트 조회 서비스 — isTradeable=true인 세트만 노출 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetService {

    private final EquipmentSetRepository equipmentSetRepository;

    /** 세트 목록 — 이름 검색 + 페이징 */
    public Page<SetResponse> getSets(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return equipmentSetRepository.findByNameContainingAndIsTradeableTrue(name, pageable)
                    .map(SetResponse::from);
        }
        return equipmentSetRepository.findByIsTradeableTrue(pageable).map(SetResponse::from);
    }

    /** 세트 단건 조회 — isTradeable=false이면 404 */
    public SetResponse getSet(Long id) {
        return equipmentSetRepository.findById(id)
                .filter(s -> s.isTradeable())
                .map(SetResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "세트를 찾을 수 없습니다. id=" + id));
    }
}
