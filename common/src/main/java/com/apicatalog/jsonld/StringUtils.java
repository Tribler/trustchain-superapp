package com.apicatalog.jsonld;

public final class StringUtils {

    private StringUtils() {
        // protected
    }

    public static boolean isBlank(final String string) {
        return string == null || string.trim().equals("");
    }

    public static boolean isNotBlank(final String string) {
        return !isBlank(string);
    }

    public static String strip(final String string) {
        return string.trim();
    }

    public static String stripTrailing(final String string) {
        String strippedTrail = string;
        while (strippedTrail.endsWith(" ") || strippedTrail.endsWith("\n")) {
            strippedTrail = strippedTrail.substring(0, strippedTrail.length() - 1);
        }
        return strippedTrail;
    }

}
