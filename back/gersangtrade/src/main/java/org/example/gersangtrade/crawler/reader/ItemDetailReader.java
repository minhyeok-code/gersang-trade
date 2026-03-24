package org.example.gersangtrade.crawler.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.domain.catalog.Item;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Job 1 - Step 2: 이미지 URL이 없는 아이템을 DB에서 읽는 ItemReader.
 *
 * <p>Step 시작 시 imageUrl IS NULL인 전체 아이템을 메모리에 로드하고,
 * ItemDetailWriter가 하나씩 처리할 수 있도록 순서대로 반환한다.
 *
 * <p>null 반환 시 Step 자동 종료 (Spring Batch 규약).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemDetailReader implements ItemReader<Item> {

    private final ItemRepository itemRepository;

    /** 처리 대기 중인 아이템 큐. null이면 아직 초기화 안 됨 */
    private Queue<Item> queue = null;

    @Override
    public Item read() {
        // 첫 호출 시 DB에서 전체 로드
        if (queue == null) {
            queue = new LinkedList<>(itemRepository.findByImageUrlIsNull());
            log.info("ItemDetailReader 초기화: 처리 대상 아이템 {}개", queue.size());
        }

        return queue.poll();  // 큐가 비면 null 반환 → Step 종료
    }

    /** Job 재실행 시 큐 초기화 (StepScope가 없는 경우를 위한 리셋) */
    public void reset() {
        this.queue = null;
    }
}
