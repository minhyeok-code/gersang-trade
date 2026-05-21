package org.example.gersangtrade.catalog.repository;

import org.example.gersangtrade.domain.catalog.CharacteristicEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacteristicEffectRepository extends JpaRepository<CharacteristicEffect, Long> {

    List<CharacteristicEffect> findByCharacteristicId(Long characteristicId);
}
