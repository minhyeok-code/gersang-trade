# 몬스터 이미지 파이프라인 설명서

게임 스크린샷에서 몬스터 이미지를 누끼 따서 DB의 monster_id와 매핑하고, S3에 올려 서비스에서 사용하는 전체 흐름.

---

## 1. 구성 요소

| 파일 | 역할 |
|---|---|
| `map_monsters.py` | 메인 도구: 배경판 복원 → 누끼 → OCR → DB 매핑 → `{id}.png` 저장 + `review.html` 생성 |
| `apply_corrections.py` | 검수 결과(corrections.csv) 적용 + 별칭 학습 |
| `make_aliases.py` | (복구용) 이미 적용한 corrections에서 별칭만 뒤늦게 생성 |

### 영구 자산 (지우면 안 됨)

| 파일 | 위치 | 역할 |
|---|---|---|
| `aliases.csv` | 출력폴더 | OCR 오독 → id 교정 학습 데이터. 검수할수록 쌓여서 자동확정률 상승 |
| `_bg_plate.png` | 출력폴더 | 복원된 도감 배경판. 있으면 재사용 → 소량 배치도 고품질 누끼 |

출력폴더를 정리할 때 이 두 파일은 반드시 보존한다.

---

## 2. 사전 준비

```
pip install numpy pillow scipy easyocr
```

- easyocr 최초 실행 시 한글(ko)·일본어(ja) 모델을 자동 다운로드한다 (수 분 소요)
- **monsters.csv**: DB에서 추출한 몬스터 목록. 헤더 필수, `id,name` 컬럼 사용
  - name 예: `이랑진군 (水)`, `사슴` — 속성은 이름 끝 괄호로 표기
  - 같은 이름의 중복 행(크롤러 부산물)이 있으면 매칭 시 모든 id에 이미지가 복사됨

### 스크린샷 촬영 규칙

- 해상도 **1024x768**, 도감 창 위치 고정 (좌표가 하드코딩되어 있음)
- 도감 상세창(몬스터 이름이 상단에 보이는 상태)에서 촬영
- 한 폴더에 전부 모아서 한 번에 실행 (중복 촬영 무해)
- 해상도나 창 위치가 바뀌면 스크립트 상단 `ROI`, `NAME_ROI` 좌표 수정 필요

---

## 3. 기본 워크플로

```
python map_monsters.py <스크린샷폴더> <monsters.csv> <출력폴더>
```

예:

```
python map_monsters.py C:\AKInteractive\GerTest2\ScreenShots monsters.csv C:\AKInteractive\GerTest2\monsters
```

실행 결과:

- 자동확정 건 → `출력폴더/{id}.png`
- 미확정 건 → `출력폴더/unmatched/`
- `report.csv` — 전 건의 매칭 기록
- `review.html` — 미확정 건 검수 화면

### 검수

1. `review.html`을 브라우저로 연다
2. 각 항목의 이름 영역·누끼 썸네일을 보고 올바른 몬스터를 드롭다운에서 선택
   - review 건은 1순위 후보가 미리 선택돼 있어 확인만 하면 됨
   - DB에 없는 몬스터는 "폐기" 선택 (또는 DB에 추가 후 재실행)
   - 드롭다운에 없으면 id 직접 입력
3. 우상단 "corrections.csv 내보내기" 클릭

### 적용

```
python apply_corrections.py corrections.csv <출력폴더>
```

- 검수 결과대로 `{id}.png` 이동/복사
- 확정된 오독 쌍을 `aliases.csv`에 자동 학습 → 다음 실행부터 같은 오독은 자동확정

### report.csv 상태값

| status | 의미 | 조치 |
|---|---|---|
| `matched` | 이름 기준 자동확정 | 없음 |
| `matched(color)` | 색상 판별로 속성 변형 확정 | 의심되면 이미지 확인 |
| `review` | 후보는 있으나 확신 부족 | 검수에서 확인 |
| `review(색상충돌)` | 색 판별이 같은 id에 중복 매칭 | 중복 촬영이면 폐기, 아니면 올바른 속성 선택. 먼저 저장된 쪽 이미지도 확인 |
| `ambiguous` | 속성 변형 구분 실패 | 검수에서 속성 선택 |

---

## 4. 신규 몬스터 (게임 패치) 처리

1. 거상짱 크롤러로 신규 몬스터를 DB에 추가 → monsters.csv 재추출
2. 신규 몬스터만 도감에서 촬영 (몇 장이어도 됨)
3. 같은 명령으로 실행 — 배경판·별칭이 재사용되므로 소량 배치도 대량 배치와 동일 품질
4. 검수 → 적용 → S3 업로드

---

## 5. 문제 해결

**색상 판별이 계속 틀리는 몬스터** (원망신·절망신처럼 몸통이 무채색인 색놀이 변형)
→ `map_monsters.py` 상단 `COLOR_SKIP = {'원망신', '절망신'}` 에 이름 추가. 해당 이름은 항상 검수로 넘어감.

**누끼에 배경이 통째로 남기 시작함**
→ 게임 패치로 도감 UI가 바뀐 신호. `_bg_plate.png` 삭제 후 다양한 몬스터 스크린샷으로 재실행해 배경판 재생성.

**한 글자 차이로 다른 몬스터가 실존하는 경우** (오운_소 vs 오문)
→ 자동확정 안 되고 review로 빠지는 것이 정상. 검수에서 정확히 선택하면 별칭으로 학습되어 이후 자동.

**별칭이 있는데 review로 오는 경우**
→ 별칭이 가리키는 id가 현재 CSV에 없는 경우다. DB/CSV에 해당 몬스터가 있는지 확인.

---

## 6. S3 업로드

키 구조: `monsters/{id}.png`, `items/{id}.png`

```
aws s3 sync <출력폴더> s3://gersang-in-bucket-078455283652-ap-northeast-2-an/monsters/ --exclude "*" --include "*.png" --exclude "unmatched/*" --exclude "_*"
```

버킷 정책 (퍼블릭 읽기, `monsters/*`·`items/*`만):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": [
      "arn:aws:s3:::gersang-in-bucket-078455283652-ap-northeast-2-an/monsters/*",
      "arn:aws:s3:::gersang-in-bucket-078455283652-ap-northeast-2-an/items/*"
    ]
  }]
}
```

퍼블릭 액세스 차단 설정: ACL 관련 2개는 체크 유지, **정책 관련 2개만 해제**.

---

## 7. 서비스에서 이미지 사용 (규칙 기반, DB 컬럼 없음)

URL은 id로 계산 가능하므로 DB에 저장하지 않는다:

```
https://gersang-in-bucket-078455283652-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/{monsters|items}/{id}.png
```

프론트 예:

```html
<img src="https://gersang-in-bucket-078455283652-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/monsters/641.png"
     onerror="this.src='/img/placeholder.png'">
```

- 베이스 URL은 상수로 분리해 조합 함수 하나로 관리
- `onerror` 폴백 필수 — 이미지가 없는 id는 404이므로 기본 이미지로 대체
- 이미지 추가/교체 = S3 업로드만 하면 끝, DB·배포 불필요
- 추후 CloudFront 도입 시 베이스 URL 상수만 교체하면 됨

- items와 mercenaries 도 동일 / 현재 용병은 이미지 s3에 저장안됨.