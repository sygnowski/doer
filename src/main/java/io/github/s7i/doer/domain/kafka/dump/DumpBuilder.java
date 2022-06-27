package io.github.s7i.doer.domain.kafka.dump;

import static io.github.s7i.doer.Doer.console;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.flow.AbstractBuilder;
import io.github.s7i.doer.flow.Job;
import io.github.s7i.doer.flow.Task;
import io.github.s7i.doer.flow.TaskList;
import io.github.s7i.doer.flow.TaskWithContext;
import io.github.s7i.doer.manifest.Manifest;
import io.github.s7i.doer.manifest.dump.DumpManifest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(chain = true)
@Slf4j
public class DumpBuilder extends AbstractBuilder {

    @Setter
    Manifest<DumpManifest> manifest;

    @Setter
    Path workDir;

    @Override
    public Job build() {
        requireNonNull(manifest, "missing manifest");
        requireNonNull(workDir, "missing workdir");

        List<Task> list;
        if (nonNull(variant)) {
            list = new ArrayList<>();
            variant.getOptions().forEach((k, vs) -> {
                vs.forEach(v -> {
                    var variantManifest = makeCopy(manifest);
                    variantManifest.getParams().put(k, v);
                    log.debug("manifest variant: {}", variantManifest);

                    list.add(makeTask(variantManifest));
                });
            });
        } else {
            list = List.of(makeTask(manifest));
        }
        return new TaskList(list);
    }

    private Task makeTask(Manifest<DumpManifest> manifest) {
        return new TaskWithContext<>(manifest, workDir).taskRunner(this::runTask);
    }

    protected void runTask(Manifest<DumpManifest> manifest) {
        console().info("Start dumping from Kafka");
        if (manifest instanceof KafkaConfig) {
            new KafkaWorker(manifest.getSpecification(), (KafkaConfig) manifest).pool();
        } else {
            throw new IllegalStateException();
        }
    }

}
