package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거상 서버 목록 엔티티.
 * 총 13개 서버가 고정으로 존재하며, Flyway 시드로 관리된다.
 * PK(serverId)는 자동 증가가 아닌 게임 서버 번호(1~13)를 직접 사용한다.
 *
 * <pre>
 * 1=백호, 2=주작, 3=현무, 4=청룡, 5=봉황, 6=해태, 7=세종,
 * 8=신구, 9=단군, 10=비호, 11=태극, 12=화랑, 13=태왕
 * </pre>
 */
@Entity
@Table(name = "servers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Server {

    /**
     * 거상 서버 번호 (1~13 고정).
     * 자동 증가 없이 게임 내 서버 번호를 그대로 사용한다.
     */
    @Id
    @Column(name = "server_id")
    private Integer serverId;

    /** 서버명 — 예: "백호", "주작" */
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    /**
     * 서버 활성 여부.
     * false: 서비스 종료 또는 통합된 서버.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    public Server(Integer serverId, String name, boolean isActive) {
        this.serverId = serverId;
        this.name = name;
        this.isActive = isActive;
    }
}
