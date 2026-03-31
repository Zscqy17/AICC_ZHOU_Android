package com.google.projectgameface;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ZiyouInputConfig {
    public static final String[] ROW_1 = {"b", "p", "m", "f", "d", "t", "n", "l"};
    public static final String[] ROW_2 = {"g", "k", "h", "j", "q", "x", "r"};
    public static final String[] ROW_3 = {"zh", "ch", "sh", "z", "c", "s", "y", "w"};
    public static final String[] ROW_4 = {"a", "o", "e"};

    private ZiyouInputConfig() {
    }

    public static Map<String, String[]> createFinalMap() {
        Map<String, String[]> finalMap = new LinkedHashMap<>();
        finalMap.put("b", new String[]{"a", "ai", "an", "ang", "ao", "ei", "en", "eng", "i", "ian", "iao", "ie", "in", "ing", "o", "u"});
        finalMap.put("p", new String[]{"a", "ai", "an", "ang", "ao", "ei", "en", "eng", "i", "ian", "iao", "ie", "in", "ing", "o", "ou", "u"});
        finalMap.put("m", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "i", "ian", "iao", "ie", "in", "ing", "iu", "o", "ou", "u"});
        finalMap.put("f", new String[]{"a", "an", "ang", "ei", "en", "eng", "o", "ou", "u"});
        finalMap.put("d", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "i", "ia", "ian", "iao", "ie", "ing", "iu", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("t", new String[]{"a", "ai", "an", "ang", "ao", "e", "eng", "i", "ian", "iao", "ie", "ing", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("n", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "i", "ian", "iang", "iao", "ie", "in", "ing", "iu", "ong", "ou", "u", "uan", "ue", "uo", "v"});
        finalMap.put("l", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "eng", "i", "ia", "ian", "iang", "iao", "ie", "in", "ing", "iu", "ong", "ou", "u", "uan", "ue", "un", "uo", "v"});
        finalMap.put("g", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "ong", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("k", new String[]{"a", "ai", "an", "ang", "ao", "e", "en", "eng", "ong", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("h", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "ong", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("j", new String[]{"i", "ia", "ian", "iang", "iao", "ie", "in", "ing", "iong", "iu", "u", "uan", "ue", "un"});
        finalMap.put("q", new String[]{"i", "ia", "ian", "iang", "iao", "ie", "in", "ing", "iong", "iu", "u", "uan", "ue", "un"});
        finalMap.put("x", new String[]{"i", "ia", "ian", "iang", "iao", "ie", "in", "ing", "iong", "iu", "u", "uan", "ue", "un"});
        finalMap.put("zh", new String[]{"a", "ai", "an", "ang", "ao", "e", "en", "eng", "i", "ong", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("ch", new String[]{"a", "ai", "an", "ang", "ao", "e", "en", "eng", "i", "ong", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("sh", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "i", "ou", "u", "ua", "uai", "uan", "uang", "ui", "un", "uo"});
        finalMap.put("r", new String[]{"an", "ang", "ao", "e", "en", "eng", "i", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("z", new String[]{"a", "ai", "an", "ang", "ao", "e", "ei", "en", "eng", "i", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("c", new String[]{"a", "ai", "an", "ang", "ao", "e", "en", "eng", "i", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("s", new String[]{"a", "ai", "an", "ang", "ao", "e", "en", "eng", "i", "ong", "ou", "u", "uan", "ui", "un", "uo"});
        finalMap.put("y", new String[]{"a", "an", "ang", "ao", "e", "i", "in", "ing", "o", "ong", "ou", "u", "uan", "ue", "un"});
        finalMap.put("w", new String[]{"a", "ai", "an", "ang", "ei", "en", "eng", "o", "u"});
        finalMap.put("a", new String[]{""});
        finalMap.put("o", new String[]{""});
        finalMap.put("e", new String[]{""});
        return Collections.unmodifiableMap(finalMap);
    }
}