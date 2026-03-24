package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MaterialItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 재료 아이템 JPA 레포지토리.
 * 크롤러 Batch Job에서 재료 아이템 서브타입 생성 및 조회에 사용된다.
 */
public interface MaterialItemRepository extends JpaRepository<MaterialItem, Long> {

    /** itemId로 재료 아이템 존재 여부 확인 — UPSERT 패턴에서 기존 레코드 확인에 사용된다 */
    boolean existsByItemId(Long itemId);
}
