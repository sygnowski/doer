package io.github.s7i.doer.command;

import com.google.protobuf.Message;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(name = "proto")
public class ProtoProcessor implements Runnable {

    @Option(names = "-d", arity = "1..*")
    private File[] desc;

    @Option(names = "-m")
    private String messageName;

    @Option(names = "-f")
    private File protoData;

    @Override
    public void run() {
        try {
            var data = Files.readAllBytes(protoData.toPath());
            var paths = Stream.of(desc).map(d -> d.toPath()).collect(Collectors.toList());
            var json = toJson(paths, messageName, data);

            System.out.println(json);
        } catch (IOException e) {
            log.error("running command error", e);
        }
    }

    public String toJson(List<Path> descriptorPaths, String messageName, byte[] data) {
        var decoder = new Decoder();
        decoder.loadDescriptors(descriptorPaths);
        return decoder.toJson(decoder.findMessageDescriptor(messageName), data);
    }

    public Message toMessage(List<Path> descriptorPaths, String messageName, String json) {
        var decoder = new Decoder();
        decoder.loadDescriptors(descriptorPaths);
        return decoder.toMessage(decoder.findMessageDescriptor(messageName), json);
    }
}
