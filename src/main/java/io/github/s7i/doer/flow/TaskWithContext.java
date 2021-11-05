package io.github.s7i.doer.flow;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.Context.InitialParameters;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.Tracing;
import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.manifest.Specification;
import io.opentracing.Span;
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
    private Span span;

    protected void initContext() {
        if (manifest.hasFlag(Doer.FLAG_USE_TRACING)) {
            final var tracer = Tracing.INSTANCE.getTracer();

            span = tracer.
                  buildSpan("Running task")
                  .withTag("manifest", manifest.toString())
                  .withTag("workdir", workdir.toString())
                  .start();

            tracer.activateSpan(span);
            span.finish();

        }

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
