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

@Accessors(fluent = true)
@Setter
public class OutputBuilder {
    Context context;

    public Output build(OutputProvider outputProvider) {
        FileOutputCreator foc = () -> context.getBaseDir().resolve(outputProvider.getOutput());
        HttpOutputCreator http = outputProvider::getOutput;
        KafkaOutputCreator kafka = new KafkaUri(outputProvider, context);
        PipelineOutputCreator pipeline = () -> Globals.INSTANCE.getPipeline().connect(outputProvider.getOutput());


        final var factory = context.getOutputFactory();
        factory.register(OutputKind.FILE, foc);
        factory.register(OutputKind.HTTP, http);
        factory.register(OutputKind.KAFKA, kafka);
        factory.register(OutputKind.PIPELINE, pipeline);

        return factory.resolve(new UriResolver(outputProvider.getOutput()))
                .orElseThrow();
    }

}
