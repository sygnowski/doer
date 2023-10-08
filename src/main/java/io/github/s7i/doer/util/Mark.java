package io.github.s7i.doer.util;

import lombok.experimental.UtilityClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UtilityClass
public class Mark {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Param{

    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flag{

    }

}
