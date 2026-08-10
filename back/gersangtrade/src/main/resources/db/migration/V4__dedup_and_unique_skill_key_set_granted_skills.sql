-- set_granted_skills.skill_key 중복 정리 + 유니크 제약 추가
--
-- 배경: skill_key에 유니크 제약이 없어, 블루-그린 배포 시 두 인스턴스가 같은 DB에
--       동시에 시딩하면 같은 skill_key가 중복 삽입될 수 있었다. 그 결과
--       findBySkillKey(단건 기대)가 NonUniqueResult로 앱 기동을 실패시켰다.
-- 이 마이그레이션은 시더보다 먼저 실행되어 (1) 중복을 정리하고 (2) 재발을 DB에서 차단한다.

-- 1) 중복 행(각 skill_key의 큰 id)을 참조하는 FK를 대표 행(최소 id)으로 재지정
--    참조가 없으면 0건 갱신 — 무해하다.
UPDATE equipment_set_skill_effects e
JOIN set_granted_skills dup  ON e.set_granted_skill_id = dup.id
JOIN set_granted_skills keep ON keep.skill_key = dup.skill_key AND keep.id < dup.id
SET e.set_granted_skill_id = keep.id;

UPDATE skill_coefficients c
JOIN set_granted_skills dup  ON c.set_granted_skill_id = dup.id
JOIN set_granted_skills keep ON keep.skill_key = dup.skill_key AND keep.id < dup.id
SET c.set_granted_skill_id = keep.id;

-- 2) 중복 행 삭제 — 각 skill_key의 최소 id만 남긴다
DELETE s1 FROM set_granted_skills s1
JOIN set_granted_skills s2
  ON s1.skill_key = s2.skill_key AND s1.id > s2.id;

-- 3) 유니크 제약 추가 — 이후 동시 삽입 경쟁은 DB가 차단(두 번째 삽입 실패)
ALTER TABLE set_granted_skills
  ADD CONSTRAINT uq_set_granted_skills_skill_key UNIQUE (skill_key);
