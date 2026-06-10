package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.ScenarioRequest;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay.ItemScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.DpsScenarioOverlay.MercenaryScenarioOverlay;
import org.example.gersangtrade.calculator.overlay.MercenaryMode;
import org.example.gersangtrade.calculator.overlay.ScenarioItemType;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent.CharacteristicSelection;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * ScenarioRequest → DpsScenarioOverlay 변환.
 * 입력값 기본 검증을 수행하며, 도메인 검증은 DeckStateMerger에서 처리한다.
 */
@Service
public class DpsScenarioOverlayFactory {

    public DpsScenarioOverlay from(ScenarioRequest req) {
        return switch (req.type()) {
            case ITEM_SINGLE -> buildItemOverlay(req);
            case ITEM_SET -> buildItemSetOverlay(req);
            case MERCENARY -> buildMercenaryOverlay(req);
        };
    }

    private DpsScenarioOverlay buildItemOverlay(ScenarioRequest req) {
        if (req.affectedMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITEM_SINGLE 시나리오는 affectedMemberId가 필수입니다.");
        }
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITEM_SINGLE 시나리오는 lines가 필수입니다.");
        }
        return new DpsScenarioOverlay(
                new ItemScenarioOverlay(ScenarioItemType.ITEM_SINGLE, null,
                        req.affectedMemberId(), req.lines()),
                null
        );
    }

    private DpsScenarioOverlay buildItemSetOverlay(ScenarioRequest req) {
        if (req.setId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITEM_SET 시나리오는 setId가 필수입니다.");
        }
        if (req.affectedMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ITEM_SET 시나리오는 affectedMemberId가 필수입니다.");
        }
        return new DpsScenarioOverlay(
                new ItemScenarioOverlay(ScenarioItemType.ITEM_SET, req.setId(),
                        req.affectedMemberId(), req.lines() != null ? req.lines() : List.of()),
                null
        );
    }

    private DpsScenarioOverlay buildMercenaryOverlay(ScenarioRequest req) {
        if (req.mercenaryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MERCENARY 시나리오는 mercenaryId가 필수입니다.");
        }
        if (req.mode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MERCENARY 시나리오는 mode가 필수입니다.");
        }
        if (req.mode() == MercenaryMode.REPLACE && req.affectedMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "REPLACE 모드는 affectedMemberId가 필수입니다.");
        }
        if (req.level() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MERCENARY 시나리오는 level이 필수입니다.");
        }
        if (req.bonusTarget() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MERCENARY 시나리오는 bonusTarget이 필수입니다.");
        }
        if (req.bonusAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MERCENARY 시나리오는 bonusAmount가 필수입니다.");
        }
        List<CharacteristicSelection> characteristics =
                req.characteristics() != null ? req.characteristics() : List.of();
        return new DpsScenarioOverlay(
                null,
                new MercenaryScenarioOverlay(
                        req.mode(), req.mercenaryId(), req.affectedMemberId(),
                        req.level(), req.bonusTarget(), req.bonusAmount(), characteristics)
        );
    }
}
