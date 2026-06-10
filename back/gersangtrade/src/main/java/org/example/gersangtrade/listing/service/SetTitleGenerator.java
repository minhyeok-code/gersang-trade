package org.example.gersangtrade.listing.service;



import org.example.gersangtrade.domain.catalog.Ritual;

import org.example.gersangtrade.domain.catalog.enums.EquipmentSlot;

import org.example.gersangtrade.domain.listing.enums.RitualOutcome;

import org.example.gersangtrade.domain.user.enums.SetComposition;



import java.util.EnumSet;

import java.util.HashSet;

import java.util.List;

import java.util.Set;



/**

 * EQUIPMENT_SET 번들의 표시 제목을 생성하는 유틸리티.

 *

 * <p>거상 세트 거래 표기 규칙 (주석 정본 — 프론트 {@code setTitle.ts}와 동일):

 * <ul>

 *   <li>갑옷+투구 → {@code {세트명}갑투} / 주술 2개 시 {@code 2{마크}{세트명}갑투}</li>

 *   <li>장갑+요대+신발(변) → {@code 변{세트명}} / 주술 3개 시 {@code 3{마크}변{세트명}}</li>

 *   <li>반지 2개 → {@code {세트명}반쌍}</li>

 *   <li>5피스(갑투+변) → {@code 풀 {세트명}} / 변만 주술 3개 {@code 3{마크} 풀 {세트명}} /

 *       5피스 전부 주술 {@code 5{마크} 풀 {세트명}}</li>

 *   <li>5피스+반지 → {@code 풀 {세트명}반쌍} / 주술 규칙 동일하게 {@code 반쌍} 접미사</li>

 * </ul>

 * 마크가 혼재하거나 규칙 위치와 맞지 않으면 접두 숫자 없이 구성 표기만 사용한다.

 */

public class SetTitleGenerator {



    private static final Set<EquipmentSlot> GAMTU_SLOTS =

            EnumSet.of(EquipmentSlot.ARMOR, EquipmentSlot.HELMET);

    private static final Set<EquipmentSlot> BYEON_SLOTS =

            EnumSet.of(EquipmentSlot.GLOVES, EquipmentSlot.BELT, EquipmentSlot.SHOES);

    private static final Set<EquipmentSlot> FULL_ARMOR_SLOTS = EnumSet.of(

            EquipmentSlot.ARMOR, EquipmentSlot.HELMET,

            EquipmentSlot.GLOVES, EquipmentSlot.BELT, EquipmentSlot.SHOES);



    private SetTitleGenerator() {}

    /** 구성별 포함 슬롯 — 거래완료 statKey 매칭에 사용 */
    public static Set<EquipmentSlot> compositionSlots(SetComposition composition) {
        return switch (composition) {
            case GAMTU -> EnumSet.copyOf(GAMTU_SLOTS);
            case BYEON -> EnumSet.copyOf(BYEON_SLOTS);
            case BANSSANG -> EnumSet.of(EquipmentSlot.RING);
            case FULL -> EnumSet.copyOf(FULL_ARMOR_SLOTS);
            case FULL_BANSSANG -> {
                Set<EquipmentSlot> slots = EnumSet.copyOf(FULL_ARMOR_SLOTS);
                slots.add(EquipmentSlot.RING);
                yield slots;
            }
        };
    }

    /**

     * 포함 피스 1개 단위 입력.

     *

     * @param slot        장비 슬롯

     * @param displayMark 주술 표시 마크. 주술 없으면 null

     */

    public record PieceTitleInput(EquipmentSlot slot, String displayMark) {}

    /** watchKey 산출용 — composition·ritualCount·mark 묶음 */
    public record WatchInfo(SetComposition composition, int ritualCount, String mark) {}



    /**

     * 슬롯·주술 정보로 세트 표시 제목을 생성한다.

     */

    public static String generate(String setName, List<PieceTitleInput> pieces) {

        if (pieces.isEmpty()) {

            return "풀 " + setName;

        }

        SetComposition kind = inferBundleKind(pieces);

        if (kind == null) {

            return setName;

        }

        int ritualCount = inferRitualCount(pieces, kind);

        String mark = ritualCount > 0 ? singleDistinctMark(pieces) : null;

        if (ritualCount > 0 && mark == null) {

            ritualCount = 0;

        }

        return generateByKind(setName, kind, ritualCount, mark);

    }

    /**
     * watchKey 산출용 — composition·ritualCount·mark 를 한 번에 반환한다.
     * composition 판별 불가(비규칙 슬롯 조합)이면 null 반환.
     */
    public static WatchInfo resolveWatchInfo(List<PieceTitleInput> pieces) {

        SetComposition kind = inferBundleKind(pieces);

        if (kind == null) {

            return null;

        }

        int ritualCount = inferRitualCount(pieces, kind);

        String mark = ritualCount > 0 ? singleDistinctMark(pieces) : null;

        if (ritualCount > 0 && mark == null) {

            ritualCount = 0;

        }

        return new WatchInfo(kind, ritualCount, ritualCount > 0 ? mark : null);

    }



    /**

     * @deprecated 슬롯 정보 없이 마크 목록만으로 생성 — {@link #generate(String, List)} 사용 권장

     */

    @Deprecated

    public static String generate(String setName, List<String> pieceMarks, boolean hasRing) {

        List<PieceTitleInput> pieces = new java.util.ArrayList<>();

        EquipmentSlot[] fullOrder = {

                EquipmentSlot.HELMET, EquipmentSlot.ARMOR,

                EquipmentSlot.GLOVES, EquipmentSlot.BELT, EquipmentSlot.SHOES

        };

        int i = 0;

        for (; i < pieceMarks.size() && i < fullOrder.length; i++) {

            pieces.add(new PieceTitleInput(fullOrder[i], pieceMarks.get(i)));

        }

        if (hasRing && i < pieceMarks.size()) {

            pieces.add(new PieceTitleInput(EquipmentSlot.RING, pieceMarks.get(i)));

        }

        return generate(setName, pieces);

    }



    public static String generateByKind(String setName, SetComposition kind, int ritualCount, String mark) {

        return switch (kind) {

            case GAMTU -> ritualCount == 2 && mark != null

                    ? "2" + mark + setName + "갑투"

                    : setName + "갑투";

            case BYEON -> ritualCount == 3 && mark != null

                    ? "3" + mark + "변" + setName

                    : "변" + setName;

            case BANSSANG -> setName + "반쌍";

            case FULL -> {

                if (ritualCount == 5 && mark != null) yield "5" + mark + " 풀 " + setName;

                if (ritualCount == 3 && mark != null) yield "3" + mark + " 풀 " + setName;

                yield "풀 " + setName;

            }

            case FULL_BANSSANG -> {

                if (ritualCount == 5 && mark != null) yield "5" + mark + " 풀 " + setName + "반쌍";

                if (ritualCount == 3 && mark != null) yield "3" + mark + " 풀 " + setName + "반쌍";

                yield "풀 " + setName + "반쌍";

            }

        };

    }



    public static SetComposition inferBundleKind(List<PieceTitleInput> pieces) {

        Set<EquipmentSlot> included = new HashSet<>();

        for (PieceTitleInput p : pieces) {

            included.add(p.slot());

        }

        if (included.isEmpty()) {

            return null;

        }



        Set<EquipmentSlot> fullWithRing = EnumSet.copyOf(FULL_ARMOR_SLOTS);
        fullWithRing.add(EquipmentSlot.RING);

        if (hasAll(included, FULL_ARMOR_SLOTS) && included.contains(EquipmentSlot.RING)
                && hasOnly(included, fullWithRing)) {
            return SetComposition.FULL_BANSSANG;
        }
        if (hasAll(included, FULL_ARMOR_SLOTS) && hasOnly(included, FULL_ARMOR_SLOTS)) {
            return SetComposition.FULL;
        }
        if (hasAll(included, GAMTU_SLOTS) && hasOnly(included, GAMTU_SLOTS)) {
            return SetComposition.GAMTU;
        }
        if (hasAll(included, BYEON_SLOTS) && hasOnly(included, BYEON_SLOTS)) {
            return SetComposition.BYEON;
        }
        if (hasOnly(included, EnumSet.of(EquipmentSlot.RING))) {
            return SetComposition.BANSSANG;
        }

        // 규칙 외 슬롯 조합(부분 세트 등)은 null 반환 — resolveWatchInfo/statKey에서 오분류 방지
        return null;

    }



    public static int inferRitualCount(List<PieceTitleInput> pieces, SetComposition kind) {

        if (pieces.stream().noneMatch(p -> p.displayMark() != null)) {

            return 0;

        }

        if ((kind == SetComposition.FULL || kind == SetComposition.FULL_BANSSANG)

                && ritualOnlyOnSlots(pieces, FULL_ARMOR_SLOTS)) {

            return 5;

        }

        if ((kind == SetComposition.BYEON || kind == SetComposition.FULL || kind == SetComposition.FULL_BANSSANG)

                && ritualOnlyOnSlots(pieces, BYEON_SLOTS)) {

            return 3;

        }

        if (kind == SetComposition.GAMTU && ritualOnlyOnSlots(pieces, GAMTU_SLOTS)) {

            return 2;

        }

        return 0;

    }



    private static boolean ritualOnlyOnSlots(List<PieceTitleInput> pieces, Set<EquipmentSlot> slots) {

        List<PieceTitleInput> ritual = pieces.stream()

                .filter(p -> p.displayMark() != null)

                .toList();

        return ritual.size() == slots.size()

                && ritual.stream().allMatch(p -> slots.contains(p.slot()));

    }



    public static String singleDistinctMark(List<PieceTitleInput> pieces) {

        Set<String> marks = new HashSet<>();

        for (PieceTitleInput p : pieces) {

            if (p.displayMark() != null) {

                marks.add(p.displayMark());

            }

        }

        if (marks.size() != 1) {

            return null;

        }

        return marks.iterator().next();

    }



    private static boolean hasAll(Set<EquipmentSlot> included, Set<EquipmentSlot> required) {

        return included.containsAll(required);

    }



    private static boolean hasOnly(Set<EquipmentSlot> included, Set<EquipmentSlot> allowed) {
        return included.stream().allMatch(allowed::contains);
    }



    /**

     * 세트 제목용 주술 표시 마크를 조합한다.

     * 성공: {@code successMark} 그대로 (예: {@code <개양>}).

     * 대성공: {@code <대성공마크_일반성공마크>} 형식 (예: {@code <북두칠성_개양>}).

     */

    public static String buildTitleMark(Ritual ritual, RitualOutcome outcome) {

        if (outcome == RitualOutcome.GREAT_SUCCESS && ritual.getGreatSuccessMark() != null) {

            String gs = ritual.getGreatSuccessMark().replaceAll("[<>]", "");

            String s = ritual.getSuccessMark() != null

                    ? ritual.getSuccessMark().replaceAll("[<>]", "")

                    : ritual.getDisplayName().replaceAll("[<>]", "");

            return "<" + gs + "_" + s + ">";

        }

        return ritual.getSuccessMark() != null

                ? ritual.getSuccessMark()

                : "<" + ritual.getDisplayName() + ">";

    }

}

