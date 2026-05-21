package org.example.gersangtrade.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.response.MonsterAutocompleteResponse;
import org.example.gersangtrade.catalog.dto.response.MonsterResponse;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 몬스터 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonsterService {

    private final MonsterRepository monsterRepository;

    /**
     * 몬스터 목록 조회.
     * element 파라미터가 있으면 해당 속성만 필터링한다.
     */
    public List<MonsterResponse> getMonsters(Element element) {
        var monsters = (element != null)
                ? monsterRepository.findByElement(element)
                : monsterRepository.findAll();
        return monsters.stream().map(MonsterResponse::from).toList();
    }

    /**
     * 몬스터 이름 자동완성 — q를 포함하는 몬스터를 limit개 반환한다.
     */
    public List<MonsterAutocompleteResponse> searchMonsters(String q, int limit) {
        return monsterRepository.findByNameContaining(q, PageRequest.of(0, limit))
                .stream().map(MonsterAutocompleteResponse::from).toList();
    }

    /**
     * 몬스터 단건 조회.
     */
    public MonsterResponse getMonster(Long id) {
        return monsterRepository.findById(id)
                .map(MonsterResponse::from)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 몬스터입니다. id=" + id));
    }
}
