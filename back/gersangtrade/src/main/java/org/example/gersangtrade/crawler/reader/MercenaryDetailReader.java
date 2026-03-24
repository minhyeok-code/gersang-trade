package org.example.gersangtrade.crawler.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Job 1 - Step 4: 이미지 URL이 없는 용병을 DB에서 읽는 ItemReader.
 *
 * <p>Step 시작 시 imageUrl IS NULL인 용병 전체를 메모리에 로드하고 순서대로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercenaryDetailReader implements ItemReader<Mercenary> {

    private final MercenaryRepository mercenaryRepository;

    private Queue<Mercenary> queue = null;

    @Override
    public Mercenary read() {
        if (queue == null) {
            queue = new LinkedList<>(mercenaryRepository.findByImageUrlIsNull());
            log.info("MercenaryDetailReader 초기화: 처리 대상 용병 {}개", queue.size());
        }
        return queue.poll();
    }

    public void reset() {
        this.queue = null;
    }
}
