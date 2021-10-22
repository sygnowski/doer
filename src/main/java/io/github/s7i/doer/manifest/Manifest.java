package io.github.s7i.doer.manifest;

import java.util.Map;

public interface Manifest<T extends Specification> {

    T getSpecification();

    Map<String, String> getParams();
}
