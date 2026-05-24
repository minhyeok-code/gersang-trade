package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.domain.catalog.Server;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 거상 서버 13개 고정 목록 시딩.
 * serverId는 게임 내 서버 번호(1~13)를 그대로 사용한다.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ServerSeeder implements ApplicationRunner {

    private final ServerRepository serverRepository;

    private record ServerRow(int id, String name, boolean active) {}

    private static final List<ServerRow> SERVERS = List.of(
            new ServerRow(1,  "백호", true),
            new ServerRow(2,  "주작", true),
            new ServerRow(3,  "현무", true),
            new ServerRow(4,  "청룡", true),
            new ServerRow(5,  "봉황", true),
            new ServerRow(6,  "해태", true),
            new ServerRow(7,  "세종", true),
            new ServerRow(8,  "신구", true),
            new ServerRow(9,  "단군", true),
            new ServerRow(10, "비호", true),
            new ServerRow(11, "태극", true),
            new ServerRow(12, "화랑", true),
            new ServerRow(13, "태왕", true)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (serverRepository.count() > 0) {
            log.debug("서버 시딩 skip: 이미 존재");
            return;
        }

        log.info("서버 시딩 시작 ({}건)", SERVERS.size());
        for (ServerRow row : SERVERS) {
            serverRepository.save(Server.builder()
                    .serverId(row.id())
                    .name(row.name())
                    .isActive(row.active())
                    .build());
        }
        log.info("서버 시딩 완료 ({}건)", SERVERS.size());
    }
}
