package org.example.gersangtrade.domain.catalog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.gersangtrade.domain.catalog.enums.Enhancement;

/**
 * Enhancement enum ↔ Integer DB 컬럼 변환기.
 * DB에는 실제 강화 수치(0 / 5 / 10)로 저장한다.
 */
@Converter
public class EnhancementConverter implements AttributeConverter<Enhancement, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Enhancement enhancement) {
        return enhancement != null ? enhancement.getValue() : null;
    }

    @Override
    public Enhancement convertToEntityAttribute(Integer value) {
        return value != null ? Enhancement.fromValue(value) : null;
    }
}
