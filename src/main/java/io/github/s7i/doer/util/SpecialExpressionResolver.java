package io.github.s7i.doer.util;

import java.util.Optional;
import java.util.UUID;

public interface SpecialExpressionResolver {

    default String ofExpression(SpecialExpression exp) {
        switch (exp) {
            case UUID:
                return UUID.randomUUID().toString();
            case CLIPBOARD:
                return Clipboard.getString();
            case TIMESTAMP:
                return String.valueOf(System.currentTimeMillis());
            default:
                return null;
        }
    }

    default Optional<String> lookupSpecialExpression(String keyWord) {
        var specialExpression = SpecialExpression.fromKeyWord(keyWord);
        if (null == specialExpression) {
            return Optional.empty();
        }
        return Optional.ofNullable(ofExpression(specialExpression));
    }
}
