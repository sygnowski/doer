package io.github.s7i.doer.util;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public enum SpecialExpression {
    UUID("__UUID"),
    ROW_ID("__#"),
    CLIPBOARD("__clipboard"),
    TIMESTAMP("__TIMESTAMP");

    public final String KEY_WORD;

    public static SpecialExpression fromKeyWord(String keyword) {
        return Arrays.stream(values())
                .filter(e -> e.KEY_WORD.equalsIgnoreCase(keyword))
                .findFirst().orElse(null);
    }
}
