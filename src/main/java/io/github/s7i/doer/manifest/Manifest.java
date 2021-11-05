package io.github.s7i.doer.manifest;

import io.github.s7i.doer.util.ParamFlagExtractor;

import java.util.Map;

public interface Manifest<T extends Specification> extends ParamFlagExtractor {

    T getSpecification();

    Map<String, String> getParams();
}
