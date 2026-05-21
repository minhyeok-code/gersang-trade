package org.example.gersangtrade.catalog.dto;

import org.example.gersangtrade.domain.catalog.Server;

/**
 * 서버 목록 응답 DTO.
 *
 * @param serverId 서버 ID (1~13)
 * @param name     서버명 (예: 백호, 주작)
 */
public record ServerResponse(Integer serverId, String name) {

    public static ServerResponse from(Server server) {
        return new ServerResponse(server.getServerId(), server.getName());
    }
}
