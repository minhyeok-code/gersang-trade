package org.example.gersangtrade.crawler.tasklet;



import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.example.gersangtrade.crawler.dto.ParsedItemDto;

import org.example.gersangtrade.crawler.parser.GersangjjangParser;

import org.example.gersangtrade.crawler.parser.GersangjjangParser.CategoryInfo;

import org.example.gersangtrade.crawler.parser.GersangjjangParser.ItemRow;

import org.example.gersangtrade.crawler.parser.GersangjjangSetParser;

import org.example.gersangtrade.crawler.parser.ItemNameParser;

import org.example.gersangtrade.crawler.service.GersangjjangCatalogUpsertService;

import org.example.gersangtrade.crawler.util.JsoupFetcher;

import org.example.gersangtrade.domain.catalog.Item;

import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

import org.jsoup.nodes.Document;

import org.springframework.batch.core.StepContribution;

import org.springframework.batch.core.scope.context.ChunkContext;

import org.springframework.batch.core.step.tasklet.Tasklet;

import org.springframework.batch.repeat.RepeatStatus;

import org.springframework.stereotype.Component;

import java.util.HashSet;

import java.util.List;

import java.util.Set;



/**

 * Job 1 - Step 1: 거상짱 아이템 목록 수집 및 UPSERT Tasklet.

 *

 * <p>URL: https://www.gersangjjang.com/item/index.asp

 * 거상짱은 SSR ASP 사이트로 Jsoup 정적 파싱이 정상 동작한다.

 *

 * <p>사천왕·명왕·전설장수·주인공 전용장비 페이지는 {@link GersangjjangParser} EXCLUDED_HREFS로

 * 제외하며, 전용장비 크롤러({@link GersangjjangExclusiveEquipmentTasklet})가 별도 처리한다.

 *

 * <p>처리 흐름:

 * <ol>

 *   <li>인덱스 페이지에서 카테고리 정보(URL + 슬롯) 수집</li>

 *   <li>카테고리별 페이지 파싱 → 아이템명 추출</li>

 *   <li>보석 이름 패턴(11종) → gems 테이블 UPSERT</li>

 *   <li>슬롯이 결정된 장비 카테고리 → items(EQUIPMENT) + equipment_items UPSERT</li>

 *   <li>슬롯 미결정 카테고리 → items(MATERIAL) UPSERT</li>

 * </ol>

 */

@Slf4j

@Component

@RequiredArgsConstructor

public class GersangjjangItemTasklet implements Tasklet {



    private static final String INDEX_URL = "https://www.gersangjjang.com/item/index.asp";



    private final JsoupFetcher jsoupFetcher;

    private final GersangjjangCatalogUpsertService catalogUpsertService;



    @Override

    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)

            throws Exception {



        log.info("=== GersangjjangItemTasklet 시작: 거상짱 아이템 목록 수집 ===");



        Document indexDoc = jsoupFetcher.fetch(INDEX_URL);

        List<CategoryInfo> categories = GersangjjangParser.parseCategoryLinks(indexDoc);

        log.info("카테고리 {}개 수집 시작", categories.size());



        Set<String> processedNames = new HashSet<>();

        int gemCount = 0, equipmentCount = 0, materialCount = 0, skipCount = 0;



        for (CategoryInfo category : categories) {

            try {

                Document categoryDoc = jsoupFetcher.fetch(category.url());

                List<ItemRow> rows = GersangjjangParser.parseItemRows(categoryDoc);



                if (rows.isEmpty()) {

                    String bodySnippet = categoryDoc.body() != null

                            ? categoryDoc.body().html().replaceAll("\\s+", " ").substring(

                                    0, Math.min(300, categoryDoc.body().html().length()))

                            : "(body 없음)";

                    log.warn("카테고리 아이템 0개 (HTML 구조 확인 필요): {}\n  HTML snippet: {}",

                            category.url(), bodySnippet);

                }



                for (ItemRow row : rows) {

                    if (!processedNames.add(row.name())) continue;



                    ParsedItemDto parsed = ItemNameParser.parse(row.name()).orElse(null);

                    if (parsed == null) {

                        skipCount++;

                        continue;

                    }



                    if (parsed.type() == ParsedItemDto.ParsedType.GEM) {

                        catalogUpsertService.upsertGem(parsed);

                        gemCount++;

                    } else if (category.isMixed()) {

                        EquipmentSlot detectedSlot = GersangjjangSetParser

                                .detectSlotByName(parsed.cleanName()).orElse(null);

                        if (detectedSlot != null) {

                            Item item = catalogUpsertService.upsertEquipmentItem(

                                    parsed.cleanName(), detectedSlot, category.kind());

                            catalogUpsertService.upsertAllStats(item, row.stats());

                            catalogUpsertService.upsertSkills(item, row.skills());

                            equipmentCount++;

                        } else {

                            log.warn("슬롯 감지 실패 — MATERIAL로 저장: [{}] ({})",

                                    parsed.cleanName(), category.text());

                            Item item = catalogUpsertService.upsertMaterialItem(parsed.cleanName());

                            catalogUpsertService.upsertAllStats(item, row.stats());

                            catalogUpsertService.upsertSkills(item, row.skills());

                            materialCount++;

                        }

                    } else if (category.isEquipment()) {

                        Item item = catalogUpsertService.upsertEquipmentItem(

                                parsed.cleanName(), category.slot(), category.kind());

                        catalogUpsertService.upsertAllStats(item, row.stats());

                        catalogUpsertService.upsertSkills(item, row.skills());

                        equipmentCount++;

                    } else {

                        Item item = catalogUpsertService.upsertMaterialItem(parsed.cleanName());

                        catalogUpsertService.upsertAllStats(item, row.stats());

                        catalogUpsertService.upsertSkills(item, row.skills());

                        materialCount++;

                    }

                }



                log.debug("카테고리 완료: [{}] {} → {}개",

                        category.text(), category.url(), rows.size());



            } catch (Exception e) {

                log.warn("카테고리 파싱 실패 (skip): {} — {}", category.url(), e.getMessage());

            }

        }



        log.info("=== GersangjjangItemTasklet 완료: 보석 {}개, 장비 {}개, 재료 {}개, skip {}개 ===",

                gemCount, equipmentCount, materialCount, skipCount);

        return RepeatStatus.FINISHED;

    }

}


