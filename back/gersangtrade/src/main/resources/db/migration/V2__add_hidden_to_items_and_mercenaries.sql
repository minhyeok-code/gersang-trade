-- items·mercenaries 공개 노출 숨김 컬럼 추가.
-- 기존 행은 이미지 백필 전까지 숨김(true=b'1')이 기본값 — 이미지 sync 시 false로 전환된다.
-- monsters 테이블은 V1에 이미 hidden 컬럼이 존재하므로 대상 아님.

ALTER TABLE `items`
    ADD COLUMN `hidden` bit(1) NOT NULL DEFAULT b'1';

ALTER TABLE `mercenaries`
    ADD COLUMN `hidden` bit(1) NOT NULL DEFAULT b'1';
