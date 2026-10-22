package com.gathin.livetv;

import java.util.ArrayList;
import java.util.List;

/** The languages the iptv-org playlists come in, with the names to show for them. */
final class Languages {

    /** code, name. The user's own three come first, then the rest by name. */
    private static final String[][] TABLE = {
            {"tel", "Telugu"},
            {"hin", "Hindi"},
            {"eng", "English"},
            {"ara", "Arabic"},
            {"ben", "Bengali"},
            {"bul", "Bulgarian"},
            {"cat", "Catalan"},
            {"ces", "Czech"},
            {"dan", "Danish"},
            {"deu", "German"},
            {"ell", "Greek"},
            {"est", "Estonian"},
            {"fas", "Persian"},
            {"fin", "Finnish"},
            {"fra", "French"},
            {"heb", "Hebrew"},
            {"hun", "Hungarian"},
            {"ind", "Indonesian"},
            {"ita", "Italian"},
            {"jpn", "Japanese"},
            {"kor", "Korean"},
            {"mal", "Malayalam"},
            {"mar", "Marathi"},
            {"nld", "Dutch"},
            {"nor", "Norwegian"},
            {"pol", "Polish"},
            {"por", "Portuguese"},
            {"ron", "Romanian"},
            {"rus", "Russian"},
            {"swe", "Swedish"},
            {"tam", "Tamil"},
            {"tha", "Thai"},
            {"tur", "Turkish"},
            {"urd", "Urdu"},
            {"vie", "Vietnamese"},
            {"zho", "Chinese"},
    };

    private Languages() {
    }

    /** The code of a language, or the code itself when it is not on the list. */
    static String label(String code) {
        if (code == null) return "";
        for (String[] row : TABLE) {
            if (row[0].equals(code)) return row[1];
        }
        return code;
    }

    /** The languages offered in the settings screen: the user's picks first, then everything else. */
    static List<String[]> options(List<String> picked) {
        List<String[]> out = new ArrayList<>();
        for (String code : picked) {
            for (String[] row : TABLE) {
                if (row[0].equals(code)) {
                    out.add(row);
                    break;
                }
            }
        }
        for (String[] row : TABLE) {
            boolean already = false;
            for (String[] done : out) {
                if (done[0].equals(row[0])) {
                    already = true;
                    break;
                }
            }
            if (!already) out.add(row);
        }
        return out;
    }
}
