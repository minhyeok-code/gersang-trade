package org.example.gersangtrade.calculator.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 평가 후보(candidateRef)를 화면 표시용 이름으로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationCandidateLabelResolver {

    private final ItemRepository itemRepository;
    private final EquipmentSetRepository equipmentSetRepository;
    private final MercenaryRepository mercenaryRepository;

    public String resolve(ScenarioItemType type, Long candidateRef) {
        if (candidateRef == null) {
            return "—";
        }
        return switch (type) {
            case ITEM_SINGLE -> itemRepository.findById(candidateRef)
                    .map(i -> i.getName())
                    .orElse("삭제된 아이템");
            case ITEM_SET -> equipmentSetRepository.findById(candidateRef)
                    .map(s -> s.getName())
                    .orElse("삭제된 세트");
            case MERCENARY -> mercenaryRepository.findById(candidateRef)
                    .map(m -> m.getName())
                    .orElse("삭제된 용병");
        };
    }
}
