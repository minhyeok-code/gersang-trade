1. 현재 상황
용병의 이름과 국적은 데이터가 잘 들어옴
하지만 그 외의 데이터(스킬, 특성 등)은 하나도 맞게 들어오지 않음
심지어 데이터가 이상하게 저장되어 있음 예시 첨부
겐노 하나히네[일본]	NONE	마법불화살
겐노 하나히네[일본]	NONE	산탄사격(공중가능)
겐노 하나히네[일본]	NONE	귀신탄 (허영)
겐노 하나히네[일본]	NONE	쇠뇌 (허영)
겐노 하나히네[일본]	NONE	2연사
겐노 하나히네[일본]	NONE	그림자이동
겐노 하나히네[일본]	NONE	격노일갈 (주박)
겐노 하나히네[일본]	NONE	침묵
겐노 하나히네[일본]	NONE	철벽 (주박)
겐노 하나히네[일본]	NONE	철벽
겐노 하나히네[일본]	NONE	청룡참 (허영)
겐노 하나히네[일본]	NONE	회복술
겐노 하나히네[일본]	NONE	인형소환술
겐노 하나히네[일본]	NONE	팬더소환술
겐노 하나히네[일본]	NONE	전풍
겐노 하나히네[일본]	NONE	지천뢰 (주박)
겐노 하나히네[일본]	NONE	생사결
겐노 하나히네[일본]	NONE	승천포 (주박)
겐노 하나히네[일본]	NONE	마력회복술
겐노 하나히네[일본]	NONE	천지독살 (허영)
겐노 하나히네[일본]	NONE	사벽술 (허영)
겐노 하나히네[일본]	NONE	신성탄 (허영)
겐노 하나히네[일본]	NONE	다중보호막
겐노 하나히네[일본]	NONE	정화
겐노 하나히네[일본]	NONE	매혹
겐노 하나히네[일본]	NONE	화염창
겐노 하나히네[일본]	NONE	화염찌르기 (주박)
겐노 하나히네[일본]	NONE	지휘
겐노 하나히네[일본]	NONE	회전창 (허영)
겐노 하나히네[일본]	NONE	화염찌르기
겐노 하나히네[일본]	NONE	격려
겐노 하나히네[일본]	NONE	인내
겐노 하나히네[일본]	NONE	맹수돌진 (허영)
겐노 하나히네[일본]	NONE	회복술
겐노 하나히네[일본]	NONE	지혜
겐노 하나히네[일본]	NONE	마력탄(공중가능) (주박)
겐노 하나히네[일본]	NONE	천벌화시 (허영)
겐노 하나히네[일본]	NONE	격노염폭 (주박)
겐노 하나히네[일본]	NONE	강강수월래 (허영)

2. 엔티티 수정
용병 스킬에는 active와 passive가 존재. 현재 엔티티에서는 이를 구분하는 컬럼이 없음. 수정필요.


3. 크롤링 url
<div class="classification-container">
        <div class="row">
            <div class="label">국적</div>
            <div class="data">
                <a href="/yongbing/korea.asp">조선</a><a href="/yongbing/japan.asp">일본</a><a href="/yongbing/china.asp">중국</a><a href="/yongbing/taiwan.asp">대만</a><a href="/yongbing/india.asp">인도</a><a href="/yongbing/mongol.asp">몽골</a>
            </div>
        </div>
        <div class="row">
            <div class="label">사천왕</div>
            <div class="data">
                <a href="/yongbing/wang/chiguo.asp">지국천왕(각)</a><a href="/yongbing/wang/damoon.asp">다문천왕(각)</a><a href="/yongbing/wang/guangmu.asp">광목천왕(각)</a><a href="/yongbing/wang/zengzhang.asp">증장천왕(각)</a>
            </div>
        </div>
        <div class="row">
            <div class="label">명왕</div>
            <div class="data">
                <a href="/yongbing/ming/hangsam.asp">항삼세명왕(각)</a><a href="/yongbing/ming/gumkang.asp">금강야차명왕(각)</a><a href="/yongbing/ming/dawei.asp">대위덕명왕(각)</a><a href="/yongbing/ming/gundari.asp">군다리명왕(각)</a><a href="/yongbing/ming/budong.asp">부동명왕</a>
            </div>
        </div>
        <div class="row">
            <div class="label">주인공</div>
            <div class="data">
                <a href="/yongbing/zhujue.asp">본캐릭터</a> <span>→</span> <a href="/yongbing/2zhujue.asp">본캐전직</a> <span>→</span> <a href="/yongbing/3zhujue.asp">본캐2차전직</a>
            </div>
        </div>
        <div class="row">
            <div class="label">병종</div>
            <div class="data column-flow">
                <div class="flow-line">
                    <a href="/yongbing/putong.asp">용병</a> <span>→</span>  <a href="/yongbing/xunlian.asp">훈련용병</a> <span>→</span> <a href="/yongbing/1jiang.asp">1차장수</a> <span>→</span> <a href="/yongbing/2jiang.asp">2차장수</a> <span>→</span> <a href="/yongbing/3jiang.asp">개조</a>/<a href="/yongbing/3jiang2.asp">각성</a> <span>→</span> <a href="/yongbing/4jiang.asp">전설장수</a>
                </div>
                <div class="flow-line">
                    <a href="/yongbing/3monster.asp">환수</a> <span>→</span> <a href="/yongbing/4monster.asp">신수</a> <span>→</span> <a href="/yongbing/6monster.asp">사천왕</a>
                </div>
                <div class="flow-line">
                    <a href="/yongbing/guaiwu.asp">몬스터용병</a> / <a href="/yongbing/guaiwu_zheng.asp">증서고용</a> <span>→</span>  <a href="/yongbing/xunlian2.asp">훈련몹</a> <span>→</span> <a href="/yongbing/2guaiwu.asp">전직몬스터</a> <span>→</span> <a href="/yongbing/5monster.asp">흉수</a> <span>→</span> <a href="/yongbing/mingwang.asp">명왕</a>
                </div>
                <div class="flow-line">
                    <a href="/yongbing/guaiwu_jing.asp">정령몹</a> / <a href="/yongbing/5monster2.asp">신환수</a>
                </div>
            </div>
        </div>

5. 


