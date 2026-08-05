package org.example.gersangtrade.admin.dto.response;

/** S3 이미지 경로 동기화 결과 — 실제 imageUrl이 갱신된 행 수. */
public record ImageSyncResult(int itemsUpdated, int monstersUpdated, int mercenariesUpdated) {}
