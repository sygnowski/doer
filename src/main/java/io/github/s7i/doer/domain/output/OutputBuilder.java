package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.Globals;
import io.github.s7i.doer.domain.kafka.output.KafkaOutputCreator;
import io.github.s7i.doer.domain.kafka.output.KafkaUri;
import io.github.s7i.doer.domain.output.creator.FileOutputCreator;
import io.github.s7i.doer.domain.output.creator.HttpOutputCreator;
import io.github.s7i.doer.domain.output.creator.PipelineOutputCreator;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(fluent = true)
@Setter
@Slf4j
public class OutputBuilder {
    Context context;

    public Output build(OutputProvider outputProvider) {
        var def = outputProvider.getOutput();

        FileOutputCreator foc = () -> context.getBaseDir().resolve(def);
        HttpOutputCreator http = outputProvider::getOutput;
        KafkaOutputCreator kafka = new KafkaUri(outputProvider, context);
        PipelineOutputCreator pipeline = () -> Globals.INSTANCE.getPipeline().connect();


        final var factory = context.getOutputFactory();
        factory.register(OutputKind.FILE, foc);
        factory.register(OutputKind.HTTP, http);
        factory.register(OutputKind.KAFKA, kafka);
        factory.register(OutputKind.PIPELINE, pipeline);

        var output = factory.resolve(new UriResolver(def)).orElseThrow();
        log.debug("resolved output {}", output);
        //TODO: auto open make configurable
        output.open();

        return output;
    }

}
