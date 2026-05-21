package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetEffectRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetPieceRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser.ParsedPiece;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser.ParsedSetEffect;
import org.example.gersangtrade.crawler.parser.GersangjjangSetParser.ParsedSetRow;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.EquipmentSetEffect;
import org.example.gersangtrade.domain.catalog.EquipmentSetPiece;
import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;
import org.jsoup.nodes.Document;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job 1 - Step 3: 거상짱 장비 세트 수집 및 UPSERT Tasklet.
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>세트 목록 페이지 파싱</li>
 *   <li>세트별로 EquipmentSet UPSERT</li>
 *   <li>N종 세트 효과 EquipmentSetEffect UPSERT</li>
 * </ol>
 *
 * <p>피스별 개별 아이템(Item/EquipmentItem)은 저장하지 않는다.
 * 거상짱 세트 페이지는 피스명을 투구/갑옷 등 통칭으로 표기하므로 실제 아이템명과 불일치한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GersangjjangSetTasklet implements Tasklet {

    // TODO: 실제 거상짱 세트 페이지 URL 확인 후 수정
    private static final String SET_URL = "https://www.gersangjjang.com/item/set.asp";

    private final JsoupFetcher jsoupFetcher;
    private final EquipmentSetRepository equipmentSetRepository;
    private final EquipmentSetEffectRepository equipmentSetEffectRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentSetPieceRepository equipmentSetPieceRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== GersangjjangSetTasklet 시작: 거상짱 장비 세트 수집 ===");

        Document doc = jsoupFetcher.fetch(SET_URL);
        List<ParsedSetRow> setRows = GersangjjangSetParser.parseSetRows(doc);
        log.info("세트 {}개 파싱 완료", setRows.size());

        int setCount = 0, effectCount = 0;

        for (ParsedSetRow setRow : setRows) {
            try {
                EquipmentSet equipmentSet = upsertEquipmentSet(setRow);
                int saved = upsertSetEffects(equipmentSet, setRow.effects());
                int linked = linkPieces(equipmentSet, setRow.pieces());
                effectCount += saved;
                setCount++;
                log.debug("세트 완료: {} (세트효과 {}줄, 피스연결 {}개)", setRow.setName(), saved, linked);
            } catch (Exception e) {
                log.warn("세트 저장 실패 (skip): {} — {}", setRow.setName(), e.getMessage());
            }
        }

        log.info("=== GersangjjangSetTasklet 완료: 세트 {}개, 세트효과 {}줄 ===", setCount, effectCount);
        return RepeatStatus.FINISHED;
    }

    /** EquipmentSet UPSERT — isTradeable은 최초 저장 시만 true로 설정 */
    private EquipmentSet upsertEquipmentSet(ParsedSetRow setRow) {
        String setName = setRow.setName();
        int totalPieces = setRow.pieces().size();
        return equipmentSetRepository.findByName(setName)
                .map(existing -> {
                    existing.updateTotalPieces(totalPieces);
                    return existing;
                })
                .orElseGet(() -> {
                    EquipmentSet saved = equipmentSetRepository.save(
                            EquipmentSet.builder()
                                    .name(setName)
                                    .totalPieces(totalPieces)
                                    .isTradeable(true)
                                    .build());
                    log.debug("세트 신규 저장: {}", setName);
                    return saved;
                });
    }

    /**
     * 세트 페이지의 슬롯 정보를 기준으로 기존 EquipmentItem을 찾아 EquipmentSetPiece로 연결한다.
     * 매칭 기준: 세트명 prefix + 슬롯. 후보가 2개 이상이면 ambiguous로 간주하고 skip.
     */
    private int linkPieces(EquipmentSet equipmentSet, List<ParsedPiece> pieces) {
        int linked = 0;
        for (ParsedPiece piece : pieces) {
            EquipmentSlot slot = piece.slot();

            // 이미 연결된 슬롯은 skip
            if (equipmentSetPieceRepository.existsByEquipmentSetIdAndSlot(equipmentSet.getId(), slot)) {
                continue;
            }

            List<EquipmentItem> candidates = equipmentItemRepository
                    .findUnlinkedByNamePrefixAndSlot(equipmentSet.getName(), slot);

            if (candidates.isEmpty()) {
                log.debug("피스 매칭 없음 (skip): {} slot={}", equipmentSet.getName(), slot);
                continue;
            }
            if (candidates.size() > 1) {
                log.warn("피스 후보 {}개 — ambiguous (skip): {} slot={} candidates={}",
                        candidates.size(), equipmentSet.getName(), slot,
                        candidates.stream().map(ei -> ei.getItem().getName()).toList());
                continue;
            }

            EquipmentItem equipmentItem = candidates.get(0);
            equipmentItem.updateEquipmentSet(equipmentSet);
            equipmentSetPieceRepository.save(EquipmentSetPiece.builder()
                    .equipmentSet(equipmentSet)
                    .slot(slot)
                    .equipmentItem(equipmentItem)
                    .pieceCount(piece.pieceCount())
                    .build());
            log.debug("피스 연결: {} slot={} → {}", equipmentSet.getName(), slot, equipmentItem.getItem().getName());
            linked++;
        }
        return linked;
    }

    /** 세트 효과 UPSERT — (set_id, requiredPieces, statType, element) 기준. 저장된 줄 수 반환 */
    private int upsertSetEffects(EquipmentSet equipmentSet, List<ParsedSetEffect> effects) {
        int saved = 0;
        for (ParsedSetEffect effect : effects) {
            if (!equipmentSetEffectRepository.existsByEquipmentSet_IdAndRequiredPiecesAndStatTypeAndElement(
                    equipmentSet.getId(), effect.requiredPieces(), effect.statType(), effect.element())) {
                equipmentSetEffectRepository.save(EquipmentSetEffect.builder()
                        .equipmentSet(equipmentSet)
                        .requiredPieces(effect.requiredPieces())
                        .statType(effect.statType())
                        .statValue(effect.statValue())
                        .statUnit(effect.statUnit())
                        .element(effect.element())
                        .build());
                log.debug("세트효과 저장: {} {}종 {} {} {}",
                        equipmentSet.getName(), effect.requiredPieces(),
                        effect.statType(), effect.statValue(), effect.element());
                saved++;
            }
        }
        return saved;
    }
}
