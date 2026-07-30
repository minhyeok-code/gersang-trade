-- monsters 테이블에 hidden 컬럼 추가
-- 기본값 TRUE: 기존 데이터는 일단 숨김 처리 후 조건에 맞는 것만 노출로 전환
ALTER TABLE monsters ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT TRUE;

-- 자동 노출 조건:
--   1. element_value 가 있고 (속성값 존재)
--   2. 이름에 明 속성 표기가 없는 경우 (반각/전각 괄호 모두 처리)
UPDATE monsters
SET hidden = FALSE
WHERE element_value IS NOT NULL
  AND name NOT REGEXP '[(（]明[)）]';
