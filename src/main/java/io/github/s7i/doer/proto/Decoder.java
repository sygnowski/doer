package io.github.s7i.doer.proto;

import static java.util.Objects.nonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.HandledRuntimeException;
import io.github.s7i.doer.config.ProtoDescriptorContainer;
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

    public void loadDescriptors(ProtoDescriptorContainer container) {
        loadDescriptors(container.getDescriptorsPaths());
    }

    public Decoder loadDescriptors(List<Path> paths) {
        descriptors = readDescSet(paths).stream()
                .flatMap(fd -> fd.getMessageTypes().stream())
                .collect(Collectors.toList());
        return this;
    }

    public Descriptor findMessageDescriptor(String messageName) {
        var descriptor = descriptors.stream()
                .filter(d -> d.getName().equals(messageName) || d.getFullName().equals(messageName))
                .findFirst()
                .orElseThrow(() -> {
                    var msg = "can't find a message in descriptor set: " + messageName;
                    log.warn(msg);
                    return new NoSuchElementException(msg);
                });
        log.info("got descriptor: name: {}, fullName: {}", descriptor.getName(), descriptor.getFullName());
        return descriptor;
    }

    public String toJson(Descriptor descriptor, byte[] data) {
        return toJson(descriptor, data, false);
    }

    public String toJson(Descriptor descriptor, byte[] data, boolean safe) {
        try {
            if (nonNull(data) && data.length > 0) {
                var message = DynamicMessage.parseFrom(descriptor, data);
                var registry = TypeRegistry.newBuilder().add(descriptors).build();

                try {
                    var printer = JsonFormat.printer().usingTypeRegistry(registry);
                    return printer.print(message);
                } catch (InvalidProtocolBufferException ipe) {
                    var to = new JsonObject();
                    to.addProperty("doer.fallback.proto.error", ipe.getMessage());
                    to.addProperty("doer.fallback.proto.text", message.toString());
                    return new Gson().toJson(to);
                }
            }
            return "{}";
        } catch (InvalidProtocolBufferException ipe) {
            log.error("toJson", ipe);
            if (!safe) {
                log.error("parse proto from binary data", ipe);
                throw new HandledRuntimeException(ipe);
            } else {
                return "{}";
            }
        }
    }

    public String toJson(Message message) {
        try {
            var registry = TypeRegistry.newBuilder().add(descriptors).build();
            var printer = JsonFormat.printer().usingTypeRegistry(registry);
            return printer.print(message);
        } catch (InvalidProtocolBufferException ipe) {
            log.error("toJson", ipe);
            throw new HandledRuntimeException(ipe);
        }
    }

    public Message toMessage(Descriptor descriptor, byte[] data) {
        try {
            return DynamicMessage.parseFrom(descriptor, data);
        } catch (InvalidProtocolBufferException e) {
            log.error("decode bytes using descriptor {}, error: {}", descriptor.getFullName(), e);
            throw new HandledRuntimeException("Cannot make proto message: " + descriptor.getFullName());
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
            log.error("making proto message, form json:\n{} exception is:\n", json, e);
            throw new HandledRuntimeException("Cannot make proto message: " + descriptor.getFullName());
        }
    }

    public Message toMessageFromText(Descriptor descriptor, String text) {
        try {
            TextFormat.Parser parser = TextFormat.Parser.newBuilder()
                    .setTypeRegistry(com.google.protobuf.TypeRegistry.newBuilder()
                            .add(descriptors)
                            .build())
                    .build();

            var builder = DynamicMessage.newBuilder(descriptor);
            parser.merge(text, builder);

            return builder.build();
        } catch (ParseException e) {
            log.error("making proto message", e);
            throw new HandledRuntimeException("Cannot make proto message: " + descriptor.getFullName());
        }
    }

    public String toText(Message proto) {

        TextFormat.Printer printer = TextFormat.printer()
                .usingTypeRegistry(com.google.protobuf.TypeRegistry.newBuilder()
                        .add(descriptors)
                        .build());

        try {
            var builder = new StringBuilder();
            printer.print(proto, builder);
            return builder.toString();
        } catch (IOException e) {
            log.error("while making test form proto", e);
            throw new HandledRuntimeException(e);
        }
    }

    public byte[] toBinaryProto(String json, String message) {
        var md = findMessageDescriptor(message);
        return toMessage(md, json).toByteArray();
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
                log.error("oh no", e);
                throw new DoerException(e);
            }
        }
        return descriptors;
    }
}
