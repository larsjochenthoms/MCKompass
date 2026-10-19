package de.kompass4mc.learning;

import java.math.BigInteger;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Gleiche Schreibweisen ergeben denselben Schlüssel, unklare Angaben bleiben verschieden. */
final class AnswerNormalization {
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern DOTS = Pattern.compile("[+-]?[1-9][0-9]{0,2}(?:\\.[0-9]{3})+");
    private static final Pattern SPACES = Pattern.compile("[+-]?[1-9][0-9]{0,2}(?: [0-9]{3})+");

    private AnswerNormalization() {
    }

    static String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        boolean pendingSpace = false;
        for (int point : answer.codePoints().toArray()) {
            if (Character.isWhitespace(point) || Character.isSpaceChar(point)) {
                pendingSpace = !normalized.isEmpty();
            } else {
                if (pendingSpace) {
                    normalized.append(' ');
                    pendingSpace = false;
                }
                normalized.appendCodePoint(point);
            }
        }
        String result = Normalizer.normalize(normalized, Normalizer.Form.NFC).toLowerCase(Locale.GERMAN);
        if (INTEGER.matcher(result).matches() || DOTS.matcher(result).matches() || SPACES.matcher(result).matches()) {
            return new BigInteger(result.replace(".", "").replace(" ", "")).toString();
        }
        return result;
    }
}
