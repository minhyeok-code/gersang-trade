package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 용병 능력치 JPA 레포지토리.
 * gerniverse 상세 파싱(Job 1 Step 4) 결과를 저장한다.
 */
public interface MercenaryStatRepository extends JpaRepository<MercenaryStat, Long> {

    /**
     * 용병 ID로 스탯 목록 전체 조회.
     * 가성비 계산기에서 용병별 스탯 표시에 사용된다.
     */
    List<MercenaryStat> findByMercenaryId(Long mercenaryId);

    /**
     * 용병 ID + 스탯 키로 단건 조회 — UPSERT 패턴에서 기존 레코드 확인에 사용된다.
     */
    Optional<MercenaryStat> findByMercenaryIdAndStatKey(Long mercenaryId, StatType statKey);

    /**
     * 특정 스탯 타입을 보유한 모든 용병의 스탯 조회.
     * 가성비 계산기 목록 구성(저항깎 보유 용병 등)에 사용된다.
     */
    List<MercenaryStat> findByStatKey(StatType statKey);

    /**
     * 용병 ID에 해당하는 스탯 전체 삭제.
     * 크롤러 재파싱 시 스탯 목록 초기화 후 재적재에 사용된다.
     */
    void deleteByMercenaryId(Long mercenaryId);
}
