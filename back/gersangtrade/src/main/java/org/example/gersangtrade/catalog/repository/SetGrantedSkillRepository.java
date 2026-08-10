package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SetGrantedSkillRepository extends JpaRepository<SetGrantedSkill, Long> {

    Optional<SetGrantedSkill> findBySkillKey(String skillKey);

    /** 존재 여부만 확인 — 중복 데이터가 있어도 NonUniqueResult 예외 없이 안전(시딩용) */
    boolean existsBySkillKey(String skillKey);
}
