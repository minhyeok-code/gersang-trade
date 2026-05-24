package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SetGrantedSkillRepository extends JpaRepository<SetGrantedSkill, Long> {

    Optional<SetGrantedSkill> findBySkillKey(String skillKey);
}
