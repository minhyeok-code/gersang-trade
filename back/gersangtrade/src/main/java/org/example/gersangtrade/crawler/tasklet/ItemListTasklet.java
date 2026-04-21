package org.example.gersangtrade.crawler.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.GemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.RitualRepository;
import org.example.gersangtrade.crawler.dto.ParsedItemDto;
import org.example.gersangtrade.crawler.parser.ItemNameParser;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.enums.GemGrade;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Job 1 - Step 1: geota 아이템 전체 목록 수집 및 UPSERT Tasklet.
 *
 * <p>URL: https://geota.co.kr/gersang/calculator/item?serverId=1
 * (serverId=1 사용 — 아이템 목록은 서버 무관 동일)
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>geota 아이템 목록 HTML 파싱 (li.cursor-pointer 선택자)</li>
 *   <li>raw 아이템명을 ItemNameParser로 분류</li>
 *   <li>동일 기본 아이템명의 마커 정보 집계 (ritualApplicable, hasSlotOption)</li>
 *   <li>GEM → gems 테이블 UPSERT</li>
 *   <li>EQUIPMENT/UNKNOWN → items 테이블 UPSERT (type: EQUIPMENT or MATERIAL)</li>
 * </ol>
 *
 * <p>⚠ geota HTML 선택자(li.cursor-pointer)는 실제 사이트 확인 후 조정이 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemListTasklet implements Tasklet {

    private static final String GEOTA_ITEM_URL =
            "https://geota.co.kr/gersang/calculator/item?serverId=1";

    private final JsoupFetcher jsoupFetcher;
    private final ItemRepository itemRepository;
    private final GemRepository gemRepository;
    private final RitualRepository ritualRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        log.info("=== ItemListTasklet 시작: geota 아이템 목록 수집 ===");

        Document doc = jsoupFetcher.fetch(GEOTA_ITEM_URL);

        // ⚠ CSR 렌더링 이슈: geota 아이템 계산기는 Next.js CSR.
        // li.cursor-pointer 드롭다운은 검색어 입력 후에만 렌더링됨.
        // 검색어 없이 전체 URL 접근 시 0개 반환 가능성 높음.
        // 선택자 자체(클래스: "cursor-pointer p-2 hover:bg-blue-600 hover:text-white")는 검증됨.
        // TODO: 검색어 없이 드롭다운 렌더링 여부 확인 필요 (crawling-selector-validation.md 미확인 1항).
        Elements itemElements = doc.select("li.cursor-pointer");
        log.info("geota 아이템 목록 {}개 파싱됨", itemElements.size());

        // raw 이름 파싱 결과 수집
        List<ParsedItemDto> parsed = new ArrayList<>();
        for (var el : itemElements) {
            String rawName = el.text().trim();
            ItemNameParser.parse(rawName).ifPresent(parsed::add);
        }

        // EQUIPMENT 마커가 있는 아이템의 기본명 집계
        // key=cleanName, value=(ritualApplicable, hasSlotOption)
        Map<String, boolean[]> equipmentFlags = new HashMap<>();
        for (ParsedItemDto dto : parsed) {
            if (dto.type() == ParsedItemDto.ParsedType.EQUIPMENT) {
                boolean[] flags = equipmentFlags.computeIfAbsent(
                        dto.cleanName(), k -> new boolean[]{false, false});
                if (dto.ritualName() != null) flags[0] = true;  // ritualApplicable
                if (dto.hasSlotOption()) flags[1] = true;         // hasSlotOption
            }
        }

        // 중복 제거된 cleanName 기준으로 처리
        Set<String> processedNames = new HashSet<>();
        int gemCount = 0, itemCount = 0;

        for (ParsedItemDto dto : parsed) {
            // 중복 제거 키: GEM 강화됨+주술 조합은 ritualName까지 포함해야 주술별 변형이 각각 저장됨
            if (!processedNames.add(dto.cleanName() + ":" + dto.type() + ":" + dto.gemGrade() + ":" + dto.ritualName())) {
                continue;  // 동일 조합 중복 skip
            }

            if (dto.type() == ParsedItemDto.ParsedType.GEM) {
                upsertGem(dto);
                gemCount++;
            } else {
                // EQUIPMENT 또는 UNKNOWN
                boolean isEquipment = dto.type() == ParsedItemDto.ParsedType.EQUIPMENT
                        || equipmentFlags.containsKey(dto.cleanName());
                upsertItem(dto.cleanName(), isEquipment ? ItemType.EQUIPMENT : ItemType.MATERIAL);
                itemCount++;
            }
        }

        log.info("=== ItemListTasklet 완료: 보석 {}개, 아이템 {}개 처리 ===", gemCount, itemCount);
        return RepeatStatus.FINISHED;
    }

    /** 보석 UPSERT — 이름+등급+주술 조합이 없으면 신규 저장 */
    private void upsertGem(ParsedItemDto dto) {
        Long ritualId = null;
        if (dto.gemGrade() == GemGrade.강화됨 && dto.ritualName() != null) {
            ritualId = ritualRepository.findByDisplayName(dto.ritualName())
                    .map(Ritual::getId)
                    .orElse(null);
            if (ritualId == null) {
                log.debug("주술 미등록으로 보석 skip: {} - {}", dto.cleanName(), dto.ritualName());
                return;
            }
        }

        Long finalRitualId = ritualId;
        gemRepository.findByNameAndGemGradeAndRitualId(dto.cleanName(), dto.gemGrade(), ritualId)
                .ifPresentOrElse(
                        existing -> log.debug("보석 이미 존재: {} {}", dto.gemGrade(), dto.cleanName()),
                        () -> {
                            Ritual ritual = finalRitualId != null
                                    ? ritualRepository.findById(finalRitualId).orElse(null)
                                    : null;
                            gemRepository.save(Gem.builder()
                                    .name(dto.cleanName())
                                    .gemGrade(dto.gemGrade())
                                    .ritual(ritual)
                                    .build());
                            log.debug("보석 신규 저장: {} {}", dto.gemGrade(), dto.cleanName());
                        }
                );
    }

    /** 아이템 UPSERT — 이름이 없으면 신규 저장 */
    private void upsertItem(String name, ItemType type) {
        itemRepository.findByName(name)
                .ifPresentOrElse(
                        existing -> {
                            // 기존이 MATERIAL인데 EQUIPMENT로 확정된 경우 타입 업데이트
                            if (existing.getType() == ItemType.MATERIAL
                                    && type == ItemType.EQUIPMENT) {
                                existing.updateType(ItemType.EQUIPMENT);
                                log.debug("아이템 타입 업데이트 MATERIAL→EQUIPMENT: {}", name);
                            }
                        },
                        () -> {
                            itemRepository.save(Item.builder()
                                    .name(name)
                                    .type(type)
                                    .build());
                            log.debug("아이템 신규 저장: {} ({})", name, type);
                        }
                );
    }
}
