package org.example.gersangtrade.admin.dto.response;

import java.util.List;

/** 명왕 무기 SELF 속성값 스탯 정리 결과 */
public record MyeongwangWeaponStatCleanupResponse(
        int deletedCount,
        List<String> itemNames
) {}
