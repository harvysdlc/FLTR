package com.example.fltr;

import java.util.HashMap;
import java.util.Map;
public class BaybayinTranslator {

    private static final Map<String, String> baybayinMap = new HashMap<>();

    static {
        // Vowels
        baybayinMap.put("a", "ᜀ");
        baybayinMap.put("e", "ᜁ");
        baybayinMap.put("i", "ᜁ");
        baybayinMap.put("o", "ᜂ");
        baybayinMap.put("u", "ᜂ");

        // Consonant + vowel combinations
        baybayinMap.put("ka", "ᜃ");
        baybayinMap.put("ke", "ᜃᜒ");
        baybayinMap.put("ki", "ᜃᜒ");
        baybayinMap.put("ko", "ᜃᜓ");
        baybayinMap.put("ku", "ᜃᜓ");

        baybayinMap.put("ga", "ᜄ");
        baybayinMap.put("ge", "ᜄᜒ");
        baybayinMap.put("gi", "ᜄᜒ");
        baybayinMap.put("go", "ᜄᜓ");
        baybayinMap.put("gu", "ᜄᜓ");

        baybayinMap.put("nga", "ᜅ");
        baybayinMap.put("nge", "ᜅᜒ");
        baybayinMap.put("ngi", "ᜅᜒ");
        baybayinMap.put("ngo", "ᜅᜓ");
        baybayinMap.put("ngu", "ᜅᜓ");

        baybayinMap.put("ta", "ᜆ");
        baybayinMap.put("te", "ᜆᜒ");
        baybayinMap.put("ti", "ᜆᜒ");
        baybayinMap.put("to", "ᜆᜓ");
        baybayinMap.put("tu", "ᜆᜓ");

        baybayinMap.put("da", "ᜇ");
        baybayinMap.put("de", "ᜇᜒ");
        baybayinMap.put("di", "ᜇᜒ");
        baybayinMap.put("do", "ᜇᜓ");
        baybayinMap.put("du", "ᜇᜓ");

        baybayinMap.put("na", "ᜈ");
        baybayinMap.put("ne", "ᜈᜒ");
        baybayinMap.put("ni", "ᜈᜒ");
        baybayinMap.put("no", "ᜈᜓ");
        baybayinMap.put("nu", "ᜈᜓ");

        baybayinMap.put("pa", "ᜉ");
        baybayinMap.put("pe", "ᜉᜒ");
        baybayinMap.put("pi", "ᜉᜒ");
        baybayinMap.put("po", "ᜉᜓ");
        baybayinMap.put("pu", "ᜉᜓ");

        baybayinMap.put("ba", "ᜊ");
        baybayinMap.put("be", "ᜊᜒ");
        baybayinMap.put("bi", "ᜊᜒ");
        baybayinMap.put("bo", "ᜊᜓ");
        baybayinMap.put("bu", "ᜊᜓ");

        baybayinMap.put("ma", "ᜋ");
        baybayinMap.put("me", "ᜋᜒ");
        baybayinMap.put("mi", "ᜋᜒ");
        baybayinMap.put("mo", "ᜋᜓ");
        baybayinMap.put("mu", "ᜋᜓ");

        baybayinMap.put("ya", "ᜌ");
        baybayinMap.put("ye", "ᜌᜒ");
        baybayinMap.put("yi", "ᜌᜒ");
        baybayinMap.put("yo", "ᜌᜓ");
        baybayinMap.put("yu", "ᜌᜓ");

        baybayinMap.put("ra", "ᜍ");
        baybayinMap.put("re", "ᜍᜒ");
        baybayinMap.put("ri", "ᜍᜒ");
        baybayinMap.put("ro", "ᜍᜓ");
        baybayinMap.put("ru", "ᜍᜓ");

        baybayinMap.put("la", "ᜎ");
        baybayinMap.put("le", "ᜎᜒ");
        baybayinMap.put("li", "ᜎᜒ");
        baybayinMap.put("lo", "ᜎᜓ");
        baybayinMap.put("lu", "ᜎᜓ");

        baybayinMap.put("wa", "ᜏ");
        baybayinMap.put("we", "ᜏᜒ");
        baybayinMap.put("wi", "ᜏᜒ");
        baybayinMap.put("wo", "ᜏᜓ");
        baybayinMap.put("wu", "ᜏᜓ");

        baybayinMap.put("sa", "ᜐ");
        baybayinMap.put("se", "ᜐᜒ");
        baybayinMap.put("si", "ᜐᜒ");
        baybayinMap.put("so", "ᜐᜓ");
        baybayinMap.put("su", "ᜐᜓ");

        baybayinMap.put("ha", "ᜑ");
        baybayinMap.put("he", "ᜑᜒ");
        baybayinMap.put("hi", "ᜑᜒ");
        baybayinMap.put("ho", "ᜑᜓ");
        baybayinMap.put("hu", "ᜑᜓ");

        //consonants
        baybayinMap.put("k", "ᜃ᜔");
        baybayinMap.put("g", "ᜄ᜔");
        baybayinMap.put("r", "ᜍ᜔");
        baybayinMap.put("s", "ᜐ᜔");
        baybayinMap.put("b", "ᜊ᜔");
        baybayinMap.put("d", "ᜇ᜔");
        baybayinMap.put("m", "ᜋ᜔");
        baybayinMap.put("n", "ᜈ᜔");
        baybayinMap.put("p", "ᜉ᜔");
        baybayinMap.put("y", "ᜌ᜔");
        baybayinMap.put("h", "ᜑ᜔");
        baybayinMap.put("t", "ᜆ᜔");
        baybayinMap.put("ng", "ᜅ᜔");
        baybayinMap.put("w", "ᜏ᜔");


    }

    /**
     * Translates a Filipino word into Baybayin script using syllable mapping.
     * @param word Filipino word
     * @return Baybayin translation
     */
    public static String translateToBaybayin(String word) {
        word = word.toLowerCase().replaceAll("[^a-z]", "");

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < word.length()) {
            String syllable = null;

            // Try 3-letter syllable first, ensuring we don't go out of bounds
            if (i + 2 < word.length()) {
                syllable = word.substring(i, i + 3);
                if (baybayinMap.containsKey(syllable)) {
                    result.append(baybayinMap.get(syllable));
                    i += 3;
                    continue;
                }
            }

            // Then try 2-letter syllable, ensuring we don't go out of bounds
            if (i + 1 < word.length()) {
                syllable = word.substring(i, i + 2);
                if (baybayinMap.containsKey(syllable)) {
                    result.append(baybayinMap.get(syllable));
                    i += 2;
                    continue;
                }
            }

            // Try single vowel
            syllable = word.substring(i, i + 1);
            if (baybayinMap.containsKey(syllable)) {
                result.append(baybayinMap.get(syllable));
            } else {
                result.append(syllable); // fallback
            }

            i++;
        }

        return result.toString();
    }
}

