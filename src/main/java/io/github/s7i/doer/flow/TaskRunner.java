package io.github.s7i.doer.flow;

import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.manifest.Specification;

public interface TaskRunner<T extends Specification> {

    void runTask(Manifest<T> manifest);
}
