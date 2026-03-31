package com.google.projectgameface;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ZiyouCandidateRepository {
    private static final String TAG = "ZiyouCandidateRepo";
    private static final String FLAT_ASSET_FILE_NAME = "ziyou_candidates.json";
    private static final String REFERENCE_DICT_ASSET_FILE_NAME = "ziyou_reference_dict.json";
    private static final String ASSOCIATION_ASSET_FILE_NAME = "ziyou_associations.json";

    private final Map<String, List<String>> candidateMap;
    private final Map<String, List<String>> referenceQuanpinMap;
    private final Map<String, List<String>> associationMap;

    public ZiyouCandidateRepository(Context context) {
        candidateMap = loadFlatCandidateMap(context);
        referenceQuanpinMap = loadReferenceQuanpinMap(context);
        associationMap = loadAssociationMap(context);
    }

    public List<String> getCandidates(String composition) {
        if (composition == null) {
            return Collections.emptyList();
        }
        String trimmed = composition.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = normalizeComposition(trimmed);
        List<String> exactMatch = mergeCandidates(
                candidateMap.get(trimmed),
                candidateMap.get(normalized),
                referenceQuanpinMap.get(normalized));
        if (!exactMatch.isEmpty()) {
            return exactMatch;
        }

        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace >= 0 && lastSpace < trimmed.length() - 1) {
            String lastSyllable = trimmed.substring(lastSpace + 1);
            List<String> lastSyllableMatch = mergeCandidates(
                    candidateMap.get(lastSyllable),
                    referenceQuanpinMap.get(lastSyllable));
            if (!lastSyllableMatch.isEmpty()) {
                return lastSyllableMatch;
            }
        }
        return Collections.emptyList();
    }

    public List<String> getAssociations(String prefix) {
        if (prefix == null) {
            return Collections.emptyList();
        }
        String trimmed = prefix.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> associations = associationMap.get(trimmed);
        if (associations != null) {
            return associations;
        }
        return Collections.emptyList();
    }

    private Map<String, List<String>> loadFlatCandidateMap(Context context) {
        try {
            Map<String, List<String>> loadedMap = readFlatCandidateMapFromAssets(context, FLAT_ASSET_FILE_NAME);
            if (!loadedMap.isEmpty()) {
                return Collections.unmodifiableMap(loadedMap);
            }
        } catch (IOException | JSONException exception) {
            Log.w(TAG, "Failed to load asset-backed candidates, using fallback map", exception);
        }
        return createFallbackCandidateMap();
    }

    private Map<String, List<String>> loadReferenceQuanpinMap(Context context) {
        try {
            Map<String, List<String>> loadedMap = readStructuredMapFromAssets(
                    context,
                    REFERENCE_DICT_ASSET_FILE_NAME,
                    "quanpin");
            if (!loadedMap.isEmpty()) {
                return Collections.unmodifiableMap(loadedMap);
            }
        } catch (IOException | JSONException exception) {
            Log.w(TAG, "Failed to load reference quanpin candidates", exception);
        }
        return Collections.emptyMap();
    }

    private Map<String, List<String>> loadAssociationMap(Context context) {
        try {
            Map<String, List<String>> loadedMap = readStructuredMapFromAssets(
                    context,
                    ASSOCIATION_ASSET_FILE_NAME,
                    "associations");
            if (!loadedMap.isEmpty()) {
                return Collections.unmodifiableMap(loadedMap);
            }
        } catch (IOException | JSONException exception) {
            Log.w(TAG, "Failed to load association candidates", exception);
        }
        return Collections.emptyMap();
    }

    private Map<String, List<String>> readFlatCandidateMapFromAssets(Context context, String assetFileName)
            throws IOException, JSONException {
        try (InputStream inputStream = context.getAssets().open(assetFileName);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            JSONObject root = new JSONObject(readJson(bufferedReader));
            Map<String, List<String>> map = new LinkedHashMap<>();
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray values = root.getJSONArray(key);
                List<String> candidates = new ArrayList<>();
                for (int index = 0; index < values.length(); index++) {
                    candidates.add(values.getString(index));
                }
                map.put(key.toLowerCase(Locale.ROOT), Collections.unmodifiableList(candidates));
            }
            return map;
        }
    }

    private Map<String, List<String>> readStructuredMapFromAssets(
            Context context,
            String assetFileName,
            String sectionName) throws IOException, JSONException {
        try (InputStream inputStream = context.getAssets().open(assetFileName);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            JSONObject root = new JSONObject(readJson(bufferedReader));
            JSONObject section = root.optJSONObject(sectionName);
            if (section == null) {
                return Collections.emptyMap();
            }

            Map<String, List<String>> map = new LinkedHashMap<>();
            Iterator<String> keys = section.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray values = section.optJSONArray(key);
                if (values == null) {
                    continue;
                }
                List<String> candidates = new ArrayList<>();
                for (int index = 0; index < values.length(); index++) {
                    candidates.add(values.getString(index));
                }
                map.put(key.toLowerCase(Locale.ROOT), Collections.unmodifiableList(candidates));
            }
            return map;
        }
    }

    private static String readJson(BufferedReader bufferedReader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private static String normalizeComposition(String composition) {
        return composition.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    @SafeVarargs
    private static List<String> mergeCandidates(List<String>... candidateLists) {
        List<String> merged = new ArrayList<>();
        for (List<String> candidateList : candidateLists) {
            if (candidateList == null) {
                continue;
            }
            for (String candidate : candidateList) {
                if (!merged.contains(candidate)) {
                    merged.add(candidate);
                }
            }
        }
        return merged;
    }

    private static Map<String, List<String>> createFallbackCandidateMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        put(map, "ni", "你", "呢", "尼", "泥", "拟");
        put(map, "hao", "好", "号", "浩", "毫", "豪");
        put(map, "ni hao", "你好");
        put(map, "wo", "我", "握", "窝", "沃");
        put(map, "men", "们", "门", "闷", "焖");
        put(map, "wo men", "我们");
        put(map, "shi", "是", "时", "事", "市", "试");
        put(map, "de", "的", "得", "德");
        put(map, "bu", "不", "步", "布", "部");
        put(map, "yao", "要", "药", "摇", "咬");
        put(map, "wo yao", "我要");
        put(map, "ke", "可", "科", "课", "客", "刻");
        put(map, "yi", "一", "已", "以", "意", "议");
        put(map, "ke yi", "可以");
        put(map, "xie", "谢", "写", "些", "鞋");
        put(map, "xie xie", "谢谢");
        put(map, "zai", "在", "再", "载", "灾");
        put(map, "jian", "见", "件", "间", "建", "简");
        put(map, "zai jian", "再见");
        put(map, "zhong", "中", "种", "重", "众", "钟");
        put(map, "guo", "国", "过", "果", "锅");
        put(map, "zhong guo", "中国");
        put(map, "ren", "人", "认", "仁", "忍");
        put(map, "da", "大", "打", "答", "达");
        put(map, "jia", "家", "加", "假", "价");
        put(map, "da jia", "大家");
        put(map, "jin", "今", "进", "近", "金");
        put(map, "tian", "天", "田", "填", "甜");
        put(map, "jin tian", "今天");
        put(map, "ming", "明", "名", "命", "鸣");
        put(map, "ming tian", "明天");
        put(map, "xian", "现", "先", "线", "显");
        put(map, "zai xian", "在线");
        put(map, "gong", "工", "公", "功", "供");
        put(map, "zuo", "做", "作", "坐", "左");
        put(map, "gong zuo", "工作");
        put(map, "xue", "学", "雪", "血", "穴");
        put(map, "sheng", "生", "声", "省", "胜");
        put(map, "xue sheng", "学生");
        put(map, "lao", "老", "劳", "牢");
        put(map, "shi", "是", "时", "事", "市", "试");
        put(map, "lao shi", "老师");
        put(map, "ma", "吗", "妈", "马", "嘛");
        put(map, "ne", "呢", "哪", "讷");
        put(map, "ba", "吧", "把", "八", "爸");
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, List<String>> map, String key, String... values) {
        map.put(key, Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values))));
    }
}