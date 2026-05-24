package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MercenarySkillRepository extends JpaRepository<MercenarySkill, Long> {

    List<MercenarySkill> findByMercenaryId(Long mercenaryId);

    @Query("SELECT COUNT(ms) > 0 FROM MercenarySkill ms WHERE ms.mercenary.id = :mercenaryId AND ms.skillName = :skillName")
    boolean existsByMercenaryIdAndSkillName(@Param("mercenaryId") Long mercenaryId,
                                            @Param("skillName") String skillName);

    void deleteByMercenaryId(Long mercenaryId);

    /** 용병 ID + 스킬명으로 단건 조회 — skillKey 업데이트 시 사용 */
    @Query("SELECT ms FROM MercenarySkill ms WHERE ms.mercenary.id = :mercenaryId AND ms.skillName = :skillName")
    java.util.Optional<MercenarySkill> findByMercenaryIdAndSkillName(
            @Param("mercenaryId") Long mercenaryId, @Param("skillName") String skillName);

    /** skillKey가 null인 스킬 목록 조회 — 거니버스 데이터 보완 대상 선정 */
    @Query("SELECT ms FROM MercenarySkill ms WHERE ms.skillKey IS NULL")
    List<MercenarySkill> findBySkillKeyIsNull();

    /** 거니버스 skillKey로 기존 스킬 조회 — 스킬 계수 JSON과 기존 스킬 연결에 사용 */
    List<MercenarySkill> findBySkillKey(String skillKey);

    /** 스킬명으로 기존 스킬 조회 — skillKey가 아직 비어 있는 크롤링 데이터 보완용 */
    List<MercenarySkill> findBySkillName(String skillName);
}
