package com.alcohol.constant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BarBTI 问卷计分与人格类型判定。
 * <p>算法与 Demo 前端 {@code selectBarbtiOpt} 保持一致。</p>
 */
public final class BarbtiScorer {

    private BarbtiScorer() {
    }

    /** 根据 20 道题的选项索引(0-3)累加四维原始分 */
    public static Map<String, Integer> accumulateRawScores(List<Integer> answers) {
        Map<String, Integer> raw = new LinkedHashMap<>();
        for (String key : BarbtiConstants.SCORE_KEYS) {
            raw.put(key, 0);
        }
        for (int opt : answers) {
            int[] add = BarbtiConstants.OPTION_SCORE_MAP[opt];
            for (int j = 0; j < BarbtiConstants.SCORE_KEYS.length; j++) {
                raw.merge(BarbtiConstants.SCORE_KEYS[j], add[j], Integer::sum);
            }
        }
        return raw;
    }

    /** 将原始分归一化到 0-100（最高分维度为 100） */
    public static Map<String, Integer> normalize(Map<String, Integer> raw) {
        int max = raw.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        Map<String, Integer> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> out.put(k, max == 0 ? 0 : (int) Math.round(v * 100.0 / max)));
        return out;
    }

    /** 取最高维度映射人格类型（默认 NEGRONI-J / 苦味探索者） */
    public static BarbtiConstants.TypeDefinition resolveType(Map<String, Integer> raw) {
        String dominant = raw.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("bitter");
        return switch (dominant) {
            case "classic" -> new BarbtiConstants.TypeDefinition("OLD-FASHIONED-C", "经典主义 · 永恒配方派",
                    "你相信好的鸡尾酒经得起时间考验。对你来说，酒吧是熟悉的味道与可靠的配方，而不是猎奇实验。",
                    List.of("经典控", "品质派", "慢品型"));
            case "social" -> new BarbtiConstants.TypeDefinition("SPRITZ-S", "社交蝴蝶 · 派对中心型",
                    "酒对你来说是连接人的媒介。你享受热闹氛围，乐于认识新朋友，一杯酒可以开启一整晚的故事。",
                    List.of("社交达人", "氛围组", "外向型"));
            case "depth" -> new BarbtiConstants.TypeDefinition("MANHATTAN-D", "深夜沉思者 · 层次品鉴型",
                    "你喝酒像在阅读一本书——需要安静、需要专注。你追求风味层次与背后的故事，微醺是灵感的入口。",
                    List.of("深度品鉴", "夜猫子", "文艺型"));
            default -> new BarbtiConstants.TypeDefinition("NEGRONI-J", "深夜独饮型 · 苦味探索者",
                    "你偏爱复杂而微苦的口感，享受独自坐在吧台前的时刻。对你来说，一杯 Negroni 就是今晚最完美的答案。",
                    List.of("微苦控", "经典主义", "独饮派"));
        };
    }
}
