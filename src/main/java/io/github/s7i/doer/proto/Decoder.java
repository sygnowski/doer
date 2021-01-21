package io.github.s7i.doer.proto;

import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Decoder {

    private List<Descriptor> descriptors;

    public void loadDescriptors(List<Path> paths) {
        descriptors = readDescSet(paths).stream()
              .flatMap(fd -> fd.getMessageTypes().stream())
              .collect(Collectors.toList());
    }

    public Descriptor findMessageDescriptor(String messageName) {
        return descriptors.stream()
              .filter(d -> d.getName().equals(messageName))
              .findFirst()
              .orElseThrow(() -> new NoSuchElementException("can't find message in descriptor set: " + messageName));
    }

    public String toJson(Descriptor descriptor, byte[] data) {
        try {
            var message = DynamicMessage.parseFrom(descriptor, data);
            var registry = TypeRegistry.newBuilder().add(descriptors).build();

            JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(registry);
            return printer.print(message);
        } catch (InvalidProtocolBufferException ipe) {
            log.error("toJson", ipe);
            throw new RuntimeException(ipe);
        }
    }

    public Message toMessage(Descriptor descriptor, String json) {
        try {
            var builder = DynamicMessage.newBuilder(descriptor);
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
                    log.debug("registered proto descriptor: {}", fd.getFullName());
                }
            } catch (IOException | DescriptorValidationException e) {
                log.error("{}", e);
                throw new RuntimeException(e);
            }
        }
        return descriptors;
    }
}
