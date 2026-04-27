
url : https://www.gersangjjang.com/item/index.asp
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="거상 무기,방어구,의복,등 아이템 정보, 무기 제작,스킬, 생산재료 비용 등.">
    <meta name="keywords" content="온라인게임,아이템,무기,장비,거상">
    <title>아이템,무기,방어구 - 거상,거상짱</title>
    <style>
        :root {
            --bg-main: #dbdbdb;
            --bg-wrapper: #f4f4f4;
            --bg-row: #F2E9E1;
            --bg-label: #D5B59D;
            --border-color: #dad2c2;
            --link-color: #000;
            --link-hover: #d35400;
        }

        body { font-family: 'Malgun Gothic', sans-serif; margin: 0; padding: 20px 10px; color: #333; line-height: 1.5; background: var(--bg-main); }
        .info-wrapper { max-width: 800px; margin: 0 auto; background: var(--bg-wrapper); padding: 25px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); box-sizing: border-box; }

        /* [표준] 메인 제목: 언더라인 제거 */
        .main-title { font-size: 23px; font-weight: bold; margin: 5px 0 15px; color: #000; text-align: center; border-bottom: none; }

        /* [표준] 섹션 구분: 배경색 삭제 */
        .section-divider { background: none; color: #333; font-weight: bold; font-size: 15px; padding: 15px 8px 8px; text-align: center; margin-top: 10px; }

        .classification-container { display: flex; flex-direction: column; gap: 1px; background: var(--border-color); border: 1px solid var(--border-color); margin-bottom: 5px; }
        .row { display: flex; min-height: 40px; background: var(--bg-row); }

        .label { background: var(--bg-label); width: 100px; min-width: 90px; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; color: #000; text-align: center; }
        
        /* [표준] 데이터 구역: 14px 유지 */
        .data { flex: 1; padding: 10px 12px; font-size: 14px; display: flex; flex-wrap: wrap; align-items: center; }
        .data a { color: var(--link-color); text-decoration: none; font-weight: 500; margin-right: 15px; }
        .data a:hover { text-decoration: underline; color: var(--link-hover); }

        /* 기호(/) 및 (-) 밀착 간격 설정 */
        .symbol { display: inline-block; color: #333; font-weight: normal; margin-left: -11px; margin-right: 4px; padding: 0; }

        .description-area { font-size: 13px; line-height: 1.8; padding: 15px 5px; color: #444; }
        .setting-images { margin-top: 20px; text-align: center; }
        .setting-images img { max-width: 100%; height: auto; margin-bottom: 20px; border: 1px solid #ccc; }

        @media (max-width: 600px) {
            .info-wrapper { padding: 15px; }
            .label { width: 75px; min-width: 75px; font-size: 13px; }
            .data { padding: 8px 10px; font-size: 13px; }
            .data a { margin-right: 10px; }
            .symbol { margin-left: -7px; margin-right: 3px; }
        }
    </style>
</head>
<body>

    <style>
        /* [1] 상단 링크 바 (13px) */
        .gs-top-header {
            max-width: 1000px; margin: 0 auto;
            display: flex; justify-content: space-between; align-items: center;
            padding: 5px 10px; font-weight: bold; 
            font-size: 13px;
            font-family: 'Malgun Gothic', sans-serif;
        }
        .gs-top-header a { text-decoration: none !important; }
        .gs-top-left a { color: #555;}
        .gs-top-right a { color: #CD5C5C;}
        .gs-top-header .sep { color: #ccc; margin: 0 4px; font-weight: normal; }

        /* [2] 주메뉴 */
        .gs-nav-container { background-color: #000; width: 100%; box-shadow: 0 2px 8px rgba(0,0,0,0.4); font-family: 'Malgun Gothic', sans-serif; }
        .gs-nav-wrapper { max-width: 1000px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; padding: 0 10px; }
        .gs-home-logo { padding: 10px 15px; flex-shrink: 0; }
        .gs-home-logo a { text-decoration: none !important; }
        .gs-home-logo .logo-main { color: #CD5C5C; font-size: 23px; font-weight: 900; } 
        .gs-home-logo .logo-sub { color: #FFD700; font-size: 14px; font-weight: 800; margin-left: 3px; }
        
        .gs-menu-list { display: flex; list-style: none; margin: 0; padding: 0; }
        .gs-menu-item a { display: block; padding: 12px 15px; color: #eeeeee; font-size: 16px; font-weight: bold; white-space: nowrap; text-decoration: none !important; }
        .gs-menu-item a:hover { color: #fff; background-color: #222; }

        /* [3] 부속메뉴 (배경색 규칙 유지) */
        .gs-sub-nav-bar { background-color: #E0DACD; border-bottom: 1px solid #C5BBAA; width: 100%; font-family: 'Malgun Gothic', sans-serif; }
        .gs-sub-nav-wrapper { max-width: 1000px; margin: 0 auto; display: flex; flex-wrap: wrap; justify-content: center; }
        .gs-sub-item { display: flex; align-items: center; justify-content: center; box-sizing: border-box; }
        .gs-sub-item a { display: block; padding: 8px 14px; color: #444; font-size: 14px; font-weight: bold; text-align: center; text-decoration: none !important; }
        .gs-dark-bg { background-color: #D2C9B5; }

        /* [4] 유틸리티 섹션 (보텟계산기 버튼 + 검색바) */
        .gs-utility-section { max-width: 820px; margin: 15px auto; padding: 0 15px; display: flex; flex-direction: column; gap: 10px; align-items: center; }
        .gs-ad-wrapper { width: 100%; text-align: center; overflow: hidden; }
        
        /* 버튼과 검색창을 가로로 배치하는 컨테이너 */
        .gs-search-row { display: flex; align-items: center; gap: 15px; width: 100%; max-width: 600px; justify-content: center; }
        
        /* 보텟계산기 버튼 스타일 */
        .gs-btn-calc { 
            display: inline-block; padding: 0 15px; height: 34px; line-height: 34px;
            background-color: #CD5C5C; color: #fff !important; font-weight: bold; font-size: 14px;
            text-decoration: none !important; border-radius: 4px; white-space: nowrap;

        }
        .gs-btn-calc:hover { background-color: #b04a4a; }

       /* 검색바 컨테이너 */
.gs-search-container { width: 100%; max-width: 480px; margin: 0 auto; }
.gcse-search { width: 100% !important; }
.gsc-control-cse { background-color: transparent !important; border: none !important; padding: 0 !important; }
.gsc-search-box { margin: 0 !important; display: flex !important; }
.gsc-input-box {
border: 1px solid #C5BBAA !important;
border-radius: 4px 4px 4px 4px !important;
height: 34px !important;
display: flex !important; align-items: center !important;
}
button.gsc-search-button { background-color: #444 !important; border: 1px solid #444 !important; border-radius: 0 4px 4px 0 !important; height: 34px !important; }
.gsc-search-button svg { fill: #fff !important; }
.gsc-input { background: none !important; padding-left: 5px !important; } /* 텍스트 겹침 방지 */

        /* [5] 모바일 반응형 */
        @media screen and (max-width: 900px) {
            .gs-top-header { display: none !important;  }
            .gs-nav-wrapper { flex-direction: column; padding: 0; }
            .gs-home-logo { width: 100%; text-align: center; border-bottom: 1px solid #1a1a1a; }
            .gs-menu-list { width: 100%; flex-wrap: wrap; }
            .gs-menu-item { flex: 0 0 25%; border-right: 1px solid #1a1a1a; border-bottom: 1px solid #1a1a1a; box-sizing: border-box; }
            .gs-menu-item:nth-child(4n) { border-right: none; }
            .gs-menu-item a { padding: 10px 0; font-size: 15px; text-align: center; }
            
            .gs-search-row { flex-direction: column; width: 95%; }
            .gs-btn-calc { text-align: center; box-sizing: border-box; }
            .gs-search-container { width: 100%; }
            .gs-sub-item { width: 33.33%; height: 35px; border-right: 1px solid rgba(0,0,0,0.05); border-bottom: 1px solid rgba(0,0,0,0.05); }
        }
    </style>



    <div class="gs-top-header">
        <div class="gs-top-left">
            <a href="http://www.gersang.co.kr/main.gs" target="_blank">한국</a><span class="sep">|</span>
            <a href="https://gs.mangot5.com/gs/index" target="_blank">홍콩</a><span class="sep">|</span>
            <a href="http://www.thegreatmerchant.com/" target="_blank">미국</a><span class="sep">|</span>
            <a href="https://52gs.co/" target="_blank">大商人</a><span class="sep">|</span>
            <a href="https://g1567.com/" target="_blank">放浪记</a>
        </div>
        <div class="gs-top-right">
<a href="https://cafe.naver.com/gersangjjang.cafe" target="_blank">카페</a><span class="sep">|</span>
<a href="https://geota.co.kr/gersang/yukeuijeon?serverId=1" target="_blank">육의전</a><span class="sep">|</span>
<a href="https://geota.co.kr/gersang/satongpaldal?serverId=1" target="_blank">사통</a><span class="sep">|</span>
<a href="https://discord.gg/gersang" target="_blank">디스코드</a>
</div>
</div>

    <nav class="gs-nav-container">
        <div class="gs-nav-wrapper">
            <div class="gs-home-logo">
                <a href="/"><span class="logo-main">거상짱</span><span class="logo-sub">Pro</span></a>
            </div>
            <ul class="gs-menu-list">
                <li class="gs-menu-item"><a href="/yongbing/index.asp">용병</a></li>
                <li class="gs-menu-item"><a href="/skill/index.asp">스킬</a></li>
                <li class="gs-menu-item"><a href="/monster/index.asp">던전</a></li>
                <li class="gs-menu-item"><a href="/quest/index.asp">임무</a></li>
                <li class="gs-menu-item"><a href="/item/index.asp">아이템</a></li>
                <li class="gs-menu-item"><a href="/zhizuo/index1.asp">장인</a></li>
                <li class="gs-menu-item"><a href="/zhizuo/index2.asp">제조</a></li>
                <li class="gs-menu-item"><a href="/qianghua/index.asp">강화</a></li>
                <li class="gs-menu-item"><a href="/jiaohuan/index.asp">교환</a></li>
                <li class="gs-menu-item"><a href="/shangtuan/index.asp">상단</a></li>
                <li class="gs-menu-item"><a href="/system/index.asp">기타</a></li>
                <li class="gs-menu-item" style="flex:1; background:#000;"></li>
            </ul>
        </div>
    </nav>


    <nav class="gs-sub-nav-bar">
        <div class="gs-sub-nav-wrapper">
            <div class="gs-sub-item gs-dark-bg"><a href="/system/zhandouexp.asp">경험치</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/testexp.asp">테섭</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/xinyong.asp">신용도</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/gongzuoliang.asp">작업량</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/shuxing.asp">속성계산</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/dikang.asp">저항계산</a></div> 
            <div class="gs-sub-item"><a href="/system/gongming.asp">공명경험치</a></div>
            <div class="gs-sub-item"><a href="/system/shangexp.asp">상단경험치</a></div>
            <div class="gs-sub-item"><a href="/system/fuzhu.asp">몹등급</a></div>
            <div class="gs-sub-item"><a href="/system/hero.asp">영웅영혼석</a></div>
            <div class="gs-sub-item"><a href="/system/star.asp">별자리</a></div>
            <div class="gs-sub-item"><a href="/system/kaogu.asp">고고학</a></div>
       
        </div>
    </nav>
    
    <section class="gs-utility-section">
  
        <div class="gs-ad-wrapper">
            <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-1379294473958513" crossorigin="anonymous"></script>
            <ins class="adsbygoogle"
                 style="display:inline-block; width:728px; height:90px"
                 data-ad-client="ca-pub-1379294473958513"
                 data-ad-slot="1904247175"></ins>
            <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
        </div>

        <div class="gs-search-row">
            <a href="/point/index.asp" class="gs-btn-calc">보텟계산기</a> 
            <div class="gs-search-container">
                <script async src="https://cse.google.com/cse.js?cx=967d754a8bfe548d0"></script>
                <div class="gcse-search"></div>
            </div>
        </div>
    </section>



<div class="info-wrapper">
    <h1 class="main-title">아이템 / 무기 / 방어구</h1>

    <div class="section-divider"> 본캐 </div>
    <div class="classification-container">
        <div class="row">
            <div class="label">전용</div>
            <div class="data">
                <a href="z_kr1.asp">조선男</a><span class="symbol">/</span><a href="z_kr2.asp">조선女</a>
                <a href="z_jp1.asp">일본男</a><span class="symbol">/</span><a href="z_jp2.asp">일본女</a>
                <a href="z_cn1.asp">중국男</a><span class="symbol">/</span><a href="z_cn2.asp">중국女</a>
                <a href="z_tw1.asp">대만男</a><span class="symbol">/</span><a href="z_tw2.asp">대만女</a>
                <a href="z_in1.asp">인도男</a><span class="symbol">/</span><a href="z_in2.asp">인도女</a>
            </div>
        </div>
        <div class="row">
            <div class="label"></div>
            <div class="data">
                <a href="zhu_bian.asp">변신무기</a>
                <a href="zhu_jiangren.asp">장인</a>
                <a href="zhu_qita.asp">기타무기</a>
            </div>
        </div>
        <div class="row">
            <div class="label">보조</div>
            <div class="data">
                <a href="hushenfu.asp">수호부(신수,천왕,명왕)</a>
                <a href="chenghao.asp">칭호</a>
                <a href="nal.asp">날개</a>
                <a href="baozhu.asp">보주</a>
            </div>
        </div>
    </div>

    <div class="section-divider"> 무기 </div>
    <div class="classification-container">
        <div class="row">
            <div class="label">근거리</div>
            <div class="data">
                <a href="dao.asp">도검</a>  
                <a href="futou.asp">도끼</a>
                <a href="mao.asp">창극</a>
                <a href="zhua.asp">조도</a>
                <a href="chakram.asp">차크람</a>
                <a href="shuangjian.asp">쌍검</a>
                <a href="bang.asp">곤봉</a>           
                 <a href="huwan.asp">보호대</a>
            </div>
        </div>
        <div class="row">
            <div class="label">중거리</div>
            <div class="data">
                <a href="wawa.asp">인형</a>
                <a href="jipangyi.asp">지팡이</a>
                <a href="kukri.asp">쿠크리</a>
                <a href="toushisuo.asp">사냥추</a>
                <a href="feibiao.asp">표창</a>
                <a href="fozhu.asp">염주</a>
                <a href="muyu.asp">목탁</a>
                <a href="shanzi.asp">부채</a>   
                <a href="gusl.asp">구슬</a>  
                <a href="she.asp">길들인뱀</a>
            </div>
        </div>
        <div class="row">
            <div class="label"></div>
            <div class="data">
                <a href="lingdang.asp">방울</a>
                <a href="jingzi.asp">거울</a>
                <a href="hufu.asp">부적</a>
                <a href="zhen.asp">침.바늘</a>
            </div>
        </div>
        <div class="row">
            <div class="label">원거리</div>
            <div class="data">
                <a href="gongjian.asp">활.궁</a>
                <a href="huoqiang.asp">조총</a>
                <a href="shigong.asp">석궁</a>
                <a href="pao.asp">화포</a>
            </div>
        </div>
    </div>

    <div class="section-divider"> 방어구 </div>
    <div class="classification-container">
        <div class="row">
            <div class="label">방어구</div>
            <div class="data">
                <a href="kuijia.asp">갑옷</a>
                <a href="toukui.asp">투구</a>
                <a href="shoutao.asp">장갑</a>
                <a href="yaodai.asp">요대</a>
                <a href="xiezi.asp">신발</a>
                <a href="jiezhi.asp">반지</a>
                <a href="yiwu.asp">유물</a>
                <a href="set.asp">[세트효과]</a>
            </div></div>
             <div class="row">
            <div class="label">의복</div>
            <div class="data">
                <a href="yifu1.asp">의복 (cash,event)</a>
                <a href="yifu2.asp">의복 (생산,드랍)</a>
                <a href="maozi1.asp">모자 (cash,event)</a>
                <a href="maozi2.asp">모자 (생산,보상)</a>
            </div></div>
             <div class="row">
            <div class="label">속성장비</div>
            <div class="data">
                <a href="waixing.asp">장신구</a>
                <a href="wushen.asp">무신</a>
                <a href="pal.asp">팔찌</a>
                <a href="gak.asp">각반</a>
            </div>
        </div>
    </div>
    
        <div class="section-divider"> 장수 </div>
    <div class="classification-container">
        <div class="row">
            <div class="label">사천왕</div>
            <div class="data">
                <a href="wang_cg.asp">지국천왕(각성)</a>
                <a href="wang_dm.asp">다문천왕(각성)</a>
                <a href="wang_gm.asp">광목천왕(각성)</a>
                <a href="wang_zz.asp">증장천왕(각성)</a>
            </div>
        </div>
        <div class="row">
            <div class="label">명왕</div>
            <div class="data">
                <a href="ming_hang.asp">항삼세명왕(火)</a>
                <a href="ming_kum.asp">금강야차명왕(水)</a>
                <a href="ming_da.asp">대위덕명왕(風)</a>
                <a href="ming_gun.asp">군다리명왕(雷)</a>
                <a href="ming_bu.asp">부동명왕(地)</a>
            </div>
        </div>
        <div class="row">
            <div class="label">전설장수</div>
            <div class="data">
                <a href="4j_lvbu.asp">여포(火)</a>
                <a href="4j_nobu.asp">노부츠나(火)</a>
                <a href="4j_choi.asp">최무선(火)</a>
                <a href="4j_chiyome.asp">치요메(水)</a>
                <a href="4j_chosen.asp">초선(水)</a>
                <a href="4j_mazo.asp">마조(水)</a>
            </div>
        </div>
        <div class="row">
            <div class="label"></div>
            <div class="data">
                <a href="4j_meng.asp">맹획(風)</a>
                <a href="4j_boku.asp">보쿠텐(風)</a>
                <a href="4j_hong.asp">홍길동(風)</a>
                <a href="4j_zhumeng.asp">주몽(雷)</a>
                <a href="4j_hua.asp">화목란(雷)</a>
                <a href="4j_baji.asp">바지라오(土)</a>
                <a href="4j_akbar.asp">악바르(土)</a>
            </div>
        </div>
        <div class="row">
            <div class="label">각성/개조</div>
            <div class="data">
                <a href="3jiang_k.asp">조선장수</a>
                <a href="3jiang_j.asp">일본장수</a>
                <a href="3jiang_c.asp">중국장수</a>
                <a href="3jiang_t.asp">대만장수</a>
                <a href="3jiang_i.asp">인도장수</a>
            </div>
        </div>
        <div class="row">
            <div class="label">몹용병</div>
            <div class="data">
                <a href="guaiwu.asp">당나귀</a>
                <a href="jingling.asp">정령몹</a>
                <a href="huanshou.asp">환수</a>
                <a href="shenshou.asp">신수</a>
                <a href="xiong.asp">흉수</a>
            </div>
        </div>
       
    </div>

    <div class="section-divider"> 잡것 </div>
    <div class="classification-container">
        <div class="row">
            <div class="label">잡화</div>
            <div class="data">
                <a href="ti.asp">약-체력</a>
                <a href="mo.asp">약-마법력</a>
                <a href="fuhuo.asp">약-부활</a>
                <a href="shiwu.asp">음식</a>
                <a href="jiaoyipin.asp">교역품</a>
                <a href="gongju.asp">공구</a>
                <a href="root.asp">[장날장부]</a>
            </div>
        </div>
        <div class="row">
            <div class="label">소모품</div>
            <div class="data">
                <a href="cash1.asp">캐시.특수품</a>
                <a href="cash2.asp">일상소모품</a>
                <a href="cash3.asp">랜덤상자</a>
                <a href="bianshenshu.asp">변신주문서</a>
                <a href="xinyong.asp">신용도+</a>
            </div>
        </div>
        <div class="row">
            <div class="label">재료</div>
            <div class="data">
                 <a href="kuangshi.asp">광물/정수</a>
                <a href="baoshi.asp">보석</a>
                <a href="yinji.asp">판대기</a>
            </div>
        </div>
    </div>

    <div class="description-area">* 찾기 힘든 내용은 웹 상단의 검색기능을 이용하세요.
    </div>
</div>

<footer class="gs-footer">


<script>
  window.gnshbrequest = window.gnshbrequest || {cmd:[]};
  window.gnshbrequest.cmd.push(function(){
    window.gnshbrequest.forceInternalRequest();
  });
</script>
<script async src="https://cpt.geniee.jp/hb/v1/224271/3179/wrapper.min.js"></script>
<script async src="https://securepubads.g.doubleclick.net/tag/js/gpt.js"></script>


        <div class="ad-sticky1">
           <div data-cptid="1604360_gersangjjang.com_videosticky" style="display: block;">
        <script>
window.gnshbrequest.cmd.push(function() {
window.gnshbrequest.applyPassback("1604360_gersangjjang.com_videosticky", "[data-cptid='1604360_gersangjjang.com_videosticky']");
});
</script>
</div></div>
<div class="ad-sticky2">
<div data-cptid="1607901_gersangjjang.com_wipead" style="display: block;">
<script>
  window.gnshbrequest.cmd.push(function() {
    window.gnshbrequest.applyPassback("1607901_gersangjjang.com_wipead", "[data-cptid='1607901_gersangjjang.com_wipead']");
  });
</script>
</div>
</div>

<div class style="text-align:center; align-items: center; margin:10;">
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-1379294473958513" crossorigin="anonymous"></script>
            <ins class="adsbygoogle"
                 style="display:inline-block; width:728px; height:90px"
                 data-ad-client="ca-pub-1379294473958513"
                 data-ad-slot="1904247175"></ins>
            <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>

</div>

<div style=" padding:7px; text-align: center;  background: #333; color: #fff; font-size: 14px; border-radius: 5px; margin: 8px 0 8px 0; ">
 수정,보충 제보 dfbb125@gmail.com
</div>
        <div class="gs-top-btn">       
            <a href="#top"><img src="/img/gotop.png" alt="TOP" width="80" height="80"></a>
        </div>


    <div class="gs-bottom-bar">
        <a href="/about.asp" class="gs-link">사이트 소개</a>
        <p class="gs-copy">Copyright &copy; since 2010  gersangjjang.com All rights reserved</p>
    </div>


</footer>


<script async src="https://www.googletagmanager.com/gtag/js?id=UA-10355500-2"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'UA-10355500-2');
</script>

<style>
    .gs-footer { width: 100%; max-width: 800px; margin: 0 auto; padding: 10px; font-family: sans-serif;}
    
    /* 댓글 영역 */
    .ad-sticky1 { margin-bottom: 10px; text-align: center; }
    .gs-top-btn { text-align: center; padding: 10px; }
    
    /* 하단 바 */
    .gs-bottom-bar { text-align: center; border-top: 1px solid #ddd; padding-top: 10px; margin-bottom: 20px; }
    .gs-link { display: inline-block; background: #333; color: #fff; text-decoration: none; padding: 5px 40px; border-radius: 4px; font-size: 14px; }
    .gs-copy { margin-top: 15px; font-weight: bold; font-size: 13px; }

    /* 반응형 */
    @media (max-width: 800px) {
        .gs-footer { width: 95%; }
        .gs-top-btn img { width: 60px; height: 60px; }
		.ad-sticky1 { display: none !important; }
    }
	@media (max-width: 1400px) {
		.ad-sticky2 { display: none !important; }
    }
</style>
</body>
</html>


인형 카테고리로 들어갔을 때 
url https://www.gersangjjang.com/item/zhu_bian.asp


<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>변신무기 - 거상,거상짱</title>
    <meta name="keywords" content="온라인게임,거상,변신무기,장비">
<meta name="description" content="거상 본캐 변신무기 인형 정보, 생산재료, 거상짱 자료 총정리.">
    <style>
         body { background-color: #dbdbdb; margin: 0; padding: 20px 10px; font-family: 'Malgun Gothic', sans-serif;  font-size: 13px; line-height: 1.5}
        .container { max-width: 800px; margin: 0 auto; background: #fff; border: 1px solid #bbb; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.2); } 

        .main-title { background-color: #333; color: #fff; text-align: center; font-size: 14px;  padding: 8px; }
        /* 데이터 행 스타일 */
        .data-row { display: flex; border-bottom: 1px solid #ccc; background-color: #fff; min-height: 100px; }
        .data-row:last-child { border-bottom: none; }
        /* 셀 공통 스타일 */
        .cell { padding: 10px; display: flex; flex-direction: column; justify-content: center; font-size: 13px; color: #333; border-right: 1px solid #ccc; box-sizing: border-box; }
        .cell:last-child { border-right: none; }   
        /* [1] 이미지 영역: 중앙 정렬 교정 */
        .w-img { width: 90px; text-align: center; align-items: center; background-color: #EAEAEA; }
        .w-img img { display: block; margin-bottom: 5px;  background: #222; }
        /* [2] 명칭 영역: 한 줄 배치 (곡괭이 1 lv) */
        .w-name { width: 150px; text-align: center; align-items: center; }
        .w-name .name-wrap { display: block; width: 100%; line-height: 1.6; }
        .w-name strong { font-size: 14px; color: #000; margin-right: 5px; }
        /* [3] 능력치 영역 */
        .w-stat { width: 120px; text-align: center; align-items: center; line-height: 1.5; }
        /* [4] 상세정보 영역: 불필요한 줄바꿈 제거 */
        .w-info { flex: 1; line-height: 1.4; text-align: left; align-items: flex-start; padding-left: 10px; background-color: #F2F2F2; }
        .info-block { margin-bottom: 3px; width: 100%; }       
        /* 제목과 내용이 붙어서 나오도록 inline 설정 */
        .info-title { font-weight: bold;  color: #000; display: inline; } 
        .info-text { display: inline; color: #333; margin-left: 2px; }
		.tem-box { background: #e0e0e0; border: 1px solid #bbb; padding: 7px; border-radius: 5px; margin-top: 10px; }
		.top-desc {  padding: 10px;  border-bottom: 2px solid #ddd; }		

        /* --- 모바일 반응형 (650px 이하) --- */
        @media screen and (max-width: 650px) {
            .data-row { flex-direction: column; margin: 12px 10px; border: 1px solid #ccc; border-radius: 10px; overflow: hidden; height: auto; }
            .cell { width: 100% !important; border-right: none; border-bottom: 1px solid #eee; text-align: center; align-items: center; padding: 10px; }
            .w-img { background-color: #eee; }
            .w-info { border-bottom: none; text-align: left; align-items: flex-start; background-color: #fcfcfc; }
        }
    </style>
</head>
<body>

    <style>
        /* [1] 상단 링크 바 (13px) */
        .gs-top-header {
            max-width: 1000px; margin: 0 auto;
            display: flex; justify-content: space-between; align-items: center;
            padding: 5px 10px; font-weight: bold; 
            font-size: 13px;
            font-family: 'Malgun Gothic', sans-serif;
        }
        .gs-top-header a { text-decoration: none !important; }
        .gs-top-left a { color: #555;}
        .gs-top-right a { color: #CD5C5C;}
        .gs-top-header .sep { color: #ccc; margin: 0 4px; font-weight: normal; }

        /* [2] 주메뉴 */
        .gs-nav-container { background-color: #000; width: 100%; box-shadow: 0 2px 8px rgba(0,0,0,0.4); font-family: 'Malgun Gothic', sans-serif; }
        .gs-nav-wrapper { max-width: 1000px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; padding: 0 10px; }
        .gs-home-logo { padding: 10px 15px; flex-shrink: 0; }
        .gs-home-logo a { text-decoration: none !important; }
        .gs-home-logo .logo-main { color: #CD5C5C; font-size: 23px; font-weight: 900; } 
        .gs-home-logo .logo-sub { color: #FFD700; font-size: 14px; font-weight: 800; margin-left: 3px; }
        
        .gs-menu-list { display: flex; list-style: none; margin: 0; padding: 0; }
        .gs-menu-item a { display: block; padding: 12px 15px; color: #eeeeee; font-size: 16px; font-weight: bold; white-space: nowrap; text-decoration: none !important; }
        .gs-menu-item a:hover { color: #fff; background-color: #222; }

        /* [3] 부속메뉴 (배경색 규칙 유지) */
        .gs-sub-nav-bar { background-color: #E0DACD; border-bottom: 1px solid #C5BBAA; width: 100%; font-family: 'Malgun Gothic', sans-serif; }
        .gs-sub-nav-wrapper { max-width: 1000px; margin: 0 auto; display: flex; flex-wrap: wrap; justify-content: center; }
        .gs-sub-item { display: flex; align-items: center; justify-content: center; box-sizing: border-box; }
        .gs-sub-item a { display: block; padding: 8px 14px; color: #444; font-size: 14px; font-weight: bold; text-align: center; text-decoration: none !important; }
        .gs-dark-bg { background-color: #D2C9B5; }

        /* [4] 유틸리티 섹션 (보텟계산기 버튼 + 검색바) */
        .gs-utility-section { max-width: 820px; margin: 15px auto; padding: 0 15px; display: flex; flex-direction: column; gap: 10px; align-items: center; }
        .gs-ad-wrapper { width: 100%; text-align: center; overflow: hidden; }
        
        /* 버튼과 검색창을 가로로 배치하는 컨테이너 */
        .gs-search-row { display: flex; align-items: center; gap: 15px; width: 100%; max-width: 600px; justify-content: center; }
        
        /* 보텟계산기 버튼 스타일 */
        .gs-btn-calc { 
            display: inline-block; padding: 0 15px; height: 34px; line-height: 34px;
            background-color: #CD5C5C; color: #fff !important; font-weight: bold; font-size: 14px;
            text-decoration: none !important; border-radius: 4px; white-space: nowrap;

        }
        .gs-btn-calc:hover { background-color: #b04a4a; }

       /* 검색바 컨테이너 */
.gs-search-container { width: 100%; max-width: 480px; margin: 0 auto; }
.gcse-search { width: 100% !important; }
.gsc-control-cse { background-color: transparent !important; border: none !important; padding: 0 !important; }
.gsc-search-box { margin: 0 !important; display: flex !important; }
.gsc-input-box {
border: 1px solid #C5BBAA !important;
border-radius: 4px 4px 4px 4px !important;
height: 34px !important;
display: flex !important; align-items: center !important;
}
button.gsc-search-button { background-color: #444 !important; border: 1px solid #444 !important; border-radius: 0 4px 4px 0 !important; height: 34px !important; }
.gsc-search-button svg { fill: #fff !important; }
.gsc-input { background: none !important; padding-left: 5px !important; } /* 텍스트 겹침 방지 */

        /* [5] 모바일 반응형 */
        @media screen and (max-width: 900px) {
            .gs-top-header { display: none !important;  }
            .gs-nav-wrapper { flex-direction: column; padding: 0; }
            .gs-home-logo { width: 100%; text-align: center; border-bottom: 1px solid #1a1a1a; }
            .gs-menu-list { width: 100%; flex-wrap: wrap; }
            .gs-menu-item { flex: 0 0 25%; border-right: 1px solid #1a1a1a; border-bottom: 1px solid #1a1a1a; box-sizing: border-box; }
            .gs-menu-item:nth-child(4n) { border-right: none; }
            .gs-menu-item a { padding: 10px 0; font-size: 15px; text-align: center; }
            
            .gs-search-row { flex-direction: column; width: 95%; }
            .gs-btn-calc { text-align: center; box-sizing: border-box; }
            .gs-search-container { width: 100%; }
            .gs-sub-item { width: 33.33%; height: 35px; border-right: 1px solid rgba(0,0,0,0.05); border-bottom: 1px solid rgba(0,0,0,0.05); }
        }
    </style>



    <div class="gs-top-header">
        <div class="gs-top-left">
            <a href="http://www.gersang.co.kr/main.gs" target="_blank">한국</a><span class="sep">|</span>
            <a href="https://gs.mangot5.com/gs/index" target="_blank">홍콩</a><span class="sep">|</span>
            <a href="http://www.thegreatmerchant.com/" target="_blank">미국</a><span class="sep">|</span>
            <a href="https://52gs.co/" target="_blank">大商人</a><span class="sep">|</span>
            <a href="https://g1567.com/" target="_blank">放浪记</a>
        </div>
        <div class="gs-top-right">
<a href="https://cafe.naver.com/gersangjjang.cafe" target="_blank">카페</a><span class="sep">|</span>
<a href="https://geota.co.kr/gersang/yukeuijeon?serverId=1" target="_blank">육의전</a><span class="sep">|</span>
<a href="https://geota.co.kr/gersang/satongpaldal?serverId=1" target="_blank">사통</a><span class="sep">|</span>
<a href="https://discord.gg/gersang" target="_blank">디스코드</a>
</div>
</div>

    <nav class="gs-nav-container">
        <div class="gs-nav-wrapper">
            <div class="gs-home-logo">
                <a href="/"><span class="logo-main">거상짱</span><span class="logo-sub">Pro</span></a>
            </div>
            <ul class="gs-menu-list">
                <li class="gs-menu-item"><a href="/yongbing/index.asp">용병</a></li>
                <li class="gs-menu-item"><a href="/skill/index.asp">스킬</a></li>
                <li class="gs-menu-item"><a href="/monster/index.asp">던전</a></li>
                <li class="gs-menu-item"><a href="/quest/index.asp">임무</a></li>
                <li class="gs-menu-item"><a href="/item/index.asp">아이템</a></li>
                <li class="gs-menu-item"><a href="/zhizuo/index1.asp">장인</a></li>
                <li class="gs-menu-item"><a href="/zhizuo/index2.asp">제조</a></li>
                <li class="gs-menu-item"><a href="/qianghua/index.asp">강화</a></li>
                <li class="gs-menu-item"><a href="/jiaohuan/index.asp">교환</a></li>
                <li class="gs-menu-item"><a href="/shangtuan/index.asp">상단</a></li>
                <li class="gs-menu-item"><a href="/system/index.asp">기타</a></li>
                <li class="gs-menu-item" style="flex:1; background:#000;"></li>
            </ul>
        </div>
    </nav>


    <nav class="gs-sub-nav-bar">
        <div class="gs-sub-nav-wrapper">
            <div class="gs-sub-item gs-dark-bg"><a href="/system/zhandouexp.asp">경험치</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/testexp.asp">테섭</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/xinyong.asp">신용도</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/gongzuoliang.asp">작업량</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/shuxing.asp">속성계산</a></div>
            <div class="gs-sub-item gs-dark-bg"><a href="/system/dikang.asp">저항계산</a></div> 
            <div class="gs-sub-item"><a href="/system/gongming.asp">공명경험치</a></div>
            <div class="gs-sub-item"><a href="/system/shangexp.asp">상단경험치</a></div>
            <div class="gs-sub-item"><a href="/system/fuzhu.asp">몹등급</a></div>
            <div class="gs-sub-item"><a href="/system/hero.asp">영웅영혼석</a></div>
            <div class="gs-sub-item"><a href="/system/star.asp">별자리</a></div>
            <div class="gs-sub-item"><a href="/system/kaogu.asp">고고학</a></div>
       
        </div>
    </nav>
    
    <section class="gs-utility-section">
  
        <div class="gs-ad-wrapper">
            <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-1379294473958513" crossorigin="anonymous"></script>
            <ins class="adsbygoogle"
                 style="display:inline-block; width:728px; height:90px"
                 data-ad-client="ca-pub-1379294473958513"
                 data-ad-slot="1904247175"></ins>
            <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
        </div>

        <div class="gs-search-row">
            <a href="/point/index.asp" class="gs-btn-calc">보텟계산기</a> 
            <div class="gs-search-container">
                <script async src="https://cse.google.com/cse.js?cx=967d754a8bfe548d0"></script>
                <div class="gcse-search"></div>
            </div>
        </div>
    </section>



<div class="container">
    <div class="main-title">변신무기 - 본캐 전용</div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/choo.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>성훈의 인형</strong><br>100 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 120-140<br>힘+50<br>민첩+50<br>생명+50<br>지력+150<br>성훈빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">획득</span><span class="info-text">: 이벤트</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 333</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/zhong.jpg">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>종리권의 인형</strong><br>220 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>생명+150<br>지력+200<br>태선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 여의다라니부1, 원망이 깃든 검 30, 땅의속성석10, 정기의구슬(地)10, 초록색알1; 수수료1억</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 100만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/han.jpeg">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>한상자의 인형</strong><br>220 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>생명+150<br>지력+200<br>청선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 백연염주1, 루드라의 끊어진 활30, 물의속성석15, 정기의구슬(水)15, 푸른색알1; 수수료1억</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 100만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/tie.png">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>철괴리의 인형</strong><br>220 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>생명+300<br>지력+50<br>괴선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 천황봉1, 야차의 부서진창30, 바람의속성석15, 정기의구슬(風)15, 백은색알1; 수수료1억</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 100만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/zhang.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>장과로의 인형</strong><br>220 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>생명+50<br>지력+300<br>노선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 야마의지팡이1, 부러진요도30, 불의속성석15, 정기의구슬(火)15, 붉은색알1; 수수료1억</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 112만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/bian/hexiangu.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>하선고의 인형</strong><br>220 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>민첩+250<br>생명+50<br>지력+50<br>선녀빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 금골선1, 깨진구슬30, 뇌전의속성석15, 정기의구슬(雷)15, 황금색알1; 수수료1억</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 112만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="img/wawa/huxian.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>호선의인형</strong><br>210 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 180-200<br>생명+50<br>지력+250<br>호선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작(장과로)</span><span class="info-text">: 정기의구슬(雷)10, 뇌전의속성석10, 호선의번개구슬30, 뇌전구슬1, 황금색알1, 1억냥</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 112만</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="/item/img/bian/lan.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>람채화의인형</strong><br>210 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 220-250<br>민첩+350<br>지력+50<br>화선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">제작-장과로</span><span class="info-text">: 얼음구슬5, 람채화의꽃바구니30, 물의속성석10, 정기의구슬(水)10, 푸른색알1, 1억냥</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 11200</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="/item/img/bian/zao.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>조국구의비검</strong><br>210 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 220-250<br>힘+300<br>지력+50<br>도선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">무기장-도검공장</span><span class="info-text">: 화열검2, 불타버린밀서30, 불의속성석10, 정기의구슬(火)10, 붉은색알1; 화로, 망치; 작업량10만</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 11200</span>
            </div>
        </div>
    </div>

    <div class="data-row">
        <div class="cell w-img">
            <img src="/item/img/bian/lv.gif">
            10근
        </div>
        <div class="cell w-name">
            <div class="name-wrap">
                <strong>여동빈의비검</strong><br>210 lv
            </div>
        </div>
        <div class="cell w-stat">
            공 220-250<br>힘+100<br>생명+250<br>검선빙의
        </div>
        <div class="cell w-info">
            <div class="info-block">
                <span class="info-title">무기장-도검공장</span><span class="info-text">: 의천검2, 비하랑의검2, 바람의속성석25, 정기의구슬(風)25, 백은색알1; 화로, 망치; 작업량10만</span>
            </div>
            <div class="info-block">
                <span class="info-title">상점가</span><span class="info-text">: 11200</span>
            </div>
        </div>
    </div>
</div>

<footer class="gs-footer">


<script>
  window.gnshbrequest = window.gnshbrequest || {cmd:[]};
  window.gnshbrequest.cmd.push(function(){
    window.gnshbrequest.forceInternalRequest();
  });
</script>
<script async src="https://cpt.geniee.jp/hb/v1/224271/3179/wrapper.min.js"></script>
<script async src="https://securepubads.g.doubleclick.net/tag/js/gpt.js"></script>


        <div class="ad-sticky1">
           <div data-cptid="1604360_gersangjjang.com_videosticky" style="display: block;">
        <script>
window.gnshbrequest.cmd.push(function() {
window.gnshbrequest.applyPassback("1604360_gersangjjang.com_videosticky", "[data-cptid='1604360_gersangjjang.com_videosticky']");
});
</script>
</div></div>
<div class="ad-sticky2">
<div data-cptid="1607901_gersangjjang.com_wipead" style="display: block;">
<script>
  window.gnshbrequest.cmd.push(function() {
    window.gnshbrequest.applyPassback("1607901_gersangjjang.com_wipead", "[data-cptid='1607901_gersangjjang.com_wipead']");
  });
</script>
</div>
</div>

<div class style="text-align:center; align-items: center; margin:10;">
    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-1379294473958513" crossorigin="anonymous"></script>
            <ins class="adsbygoogle"
                 style="display:inline-block; width:728px; height:90px"
                 data-ad-client="ca-pub-1379294473958513"
                 data-ad-slot="1904247175"></ins>
            <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>

</div>

<div style=" padding:7px; text-align: center;  background: #333; color: #fff; font-size: 14px; border-radius: 5px; margin: 8px 0 8px 0; ">
 수정,보충 제보 dfbb125@gmail.com
</div>
        <div class="gs-top-btn">       
            <a href="#top"><img src="/img/gotop.png" alt="TOP" width="80" height="80"></a>
        </div>


    <div class="gs-bottom-bar">
        <a href="/about.asp" class="gs-link">사이트 소개</a>
        <p class="gs-copy">Copyright &copy; since 2010  gersangjjang.com All rights reserved</p>
    </div>


</footer>


<script async src="https://www.googletagmanager.com/gtag/js?id=UA-10355500-2"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'UA-10355500-2');
</script>

<style>
    .gs-footer { width: 100%; max-width: 800px; margin: 0 auto; padding: 10px; font-family: sans-serif;}
    
    /* 댓글 영역 */
    .ad-sticky1 { margin-bottom: 10px; text-align: center; }
    .gs-top-btn { text-align: center; padding: 10px; }
    
    /* 하단 바 */
    .gs-bottom-bar { text-align: center; border-top: 1px solid #ddd; padding-top: 10px; margin-bottom: 20px; }
    .gs-link { display: inline-block; background: #333; color: #fff; text-decoration: none; padding: 5px 40px; border-radius: 4px; font-size: 14px; }
    .gs-copy { margin-top: 15px; font-weight: bold; font-size: 13px; }

    /* 반응형 */
    @media (max-width: 800px) {
        .gs-footer { width: 95%; }
        .gs-top-btn img { width: 60px; height: 60px; }
		.ad-sticky1 { display: none !important; }
    }
	@media (max-width: 1400px) {
		.ad-sticky2 { display: none !important; }
    }
</style>
</body>
</html>