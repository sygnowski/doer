package io.github.s7i.doer;

import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            e.printStackTrace();
        }
    }

    public String toJson(List<Path> descriptorPaths, String messageType, byte[] data) {
        try {
            var descriptors = readDescSet(descriptorPaths).stream()
                  .flatMap(fd -> fd.getMessageTypes().stream())
                  .collect(Collectors.toList());

            var messageDesc = descriptors.stream()
                  .filter(d -> d.getName().equals(messageType))
                  .findFirst()
                  .orElseThrow();

            var message = DynamicMessage.parseFrom(messageDesc, data);
            var registry = TypeRegistry.newBuilder().add(descriptors).build();

            JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(registry);
            return printer.print(message);

        } catch (InvalidProtocolBufferException e) {
            log.error("{}", e);
            throw new RuntimeException(e);
        }
    }

    public Message toMessage(List<Path> descriptorPaths, String messageType, String json) {
        var descriptors = readDescSet(descriptorPaths).stream()
              .flatMap(fd -> fd.getMessageTypes().stream())
              .collect(Collectors.toList());

        var messageDesc = descriptors.stream()
              .filter(d -> d.getName().equals(messageType))
              .findFirst()
              .orElseThrow();

        try {
            var builder = DynamicMessage.newBuilder(messageDesc);
            JsonFormat.parser()
                  .usingTypeRegistry(TypeRegistry.newBuilder()
                        .add(descriptors)
                        .build())
                  .merge(json, builder);

            return builder.build();
        } catch (InvalidProtocolBufferException e) {
            log.error("{}", e);
            throw new RuntimeException(e);
        }

    }

    private List<FileDescriptor> readDescSet(List<Path> descriptorPaths) {
        List<FileDescriptor> descriptors = new ArrayList<>();
        for (var descriptor : descriptorPaths) {
            try (var in = Files.newInputStream(descriptor)) {
                var set = FileDescriptorSet.parseFrom(in);
                for (var fdp : set.getFileList()) {
                    var fd = FileDescriptor.buildFrom(fdp, descriptors.toArray(new FileDescriptor[0]));
                    descriptors.add(fd);
                }
            } catch (IOException | DescriptorValidationException e) {
                log.error("{}", e);
                throw new RuntimeException(e);
            }
        }
        return descriptors;
    }
}
