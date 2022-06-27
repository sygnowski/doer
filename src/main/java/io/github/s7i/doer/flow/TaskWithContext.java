package io.github.s7i.doer.flow;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.Context.InitialParameters;
import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.manifest.Specification;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(fluent = true)
public class TaskWithContext<T extends Specification> implements Task {

    protected final Manifest<T> manifest;
    protected final Path workdir;
    @Setter
    private TaskRunner<T> taskRunner;

    protected void initContext() {
        new Context.Initializer(InitialParameters.builder()
              .workDir(workdir)
              .params(manifest.getParams())
              .build());
    }

    protected void runTask(Manifest<T> manifest) {
        if (nonNull(taskRunner)) {
            taskRunner.runTask(manifest);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void execute() {
        initContext();
        runTask(manifest);
    }
}
