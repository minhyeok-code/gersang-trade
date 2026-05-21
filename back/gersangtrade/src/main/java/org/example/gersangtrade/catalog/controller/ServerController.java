package org.example.gersangtrade.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.dto.ServerResponse;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서버 목록 API.
 * 비로그인 사용자도 접근 가능하며, 프론트엔드에서 서버 필터 드롭다운에 사용된다.
 *
 * <pre>
 * GET /api/servers — 활성 서버 목록 조회 (인증 불필요)
 * </pre>
 */
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerRepository serverRepository;

    /**
     * 활성 서버 목록 조회.
     * 비로그인 상태에서도 서버 선택 필터에 사용할 수 있도록 인증 없이 허용된다.
     *
     * @return 활성 서버 응답 목록 (serverId 오름차순)
     */
    @GetMapping
    public ResponseEntity<List<ServerResponse>> listServers() {
        List<ServerResponse> servers = serverRepository.findByIsActiveTrue()
                .stream()
                .map(ServerResponse::from)
                .toList();
        return ResponseEntity.ok(servers);
    }
}
