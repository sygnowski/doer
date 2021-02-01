package io.github.s7i.doer.command;

import static java.util.Objects.nonNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.github.s7i.doer.HandledRuntimeException;
import io.github.s7i.doer.proto.Decoder;
import io.github.s7i.doer.session.Input;
import io.github.s7i.doer.session.InteractiveSession;
import io.github.s7i.doer.session.ParamStorage;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(name = "proto")
public class ProtoProcessor implements Runnable, ParamStorage {

    enum InputType {
        TEXT, JSON, BYTESTRING;
    }

    enum ExportAs {
        BIN, TEXT, JSON, BYTESTRING;

        String keyword() {
            return this.name().toLowerCase();
        }

        static ExportAs of(String value) {
            return Arrays.stream(values())
                  .filter(e -> e.keyword().equals(value))
                  .findFirst()
                  .orElseThrow();
        }

    }

    @Option(names = "-d", arity = "1..*")
    private File[] desc;

    @Option(names = "-i")
    private boolean interactive;

    @Option(names = {"-t", "--inputType"}, defaultValue = "json")
    private InputType inputType;

    @Option(names = "-m")
    private String messageName;

    @Option(names = "-f")
    private File protoData;

    @Option(names = {"-e", "--exportAs"}, defaultValue = "json")
    private String exportAs;

    @Option(names = {"-o", "--output"})
    private File output;

    @Override
    public void run() {
        try {
            if (interactive) {
                processInteractive();
            } else {

                var data = Files.readAllBytes(protoData.toPath());
                var paths = getPaths();
                var json = toJson(paths, messageName, data);
                log.info(json);
            }
        } catch (IOException e) {
            log.error("running command error", e);
        }
    }

    private void processInteractive() throws IOException {
        final var json = inputType == InputType.JSON;
        final var text = inputType == InputType.TEXT;
        var decoder = new Decoder();
        decoder.loadDescriptors(getPaths());

        var session = new InteractiveSession();
        var input = new Input();

        try (var br = new Scanner(new InputStreamReader(System.in))) {
            do {
                System.out.print("doer > ");
                while (br.hasNextLine()) {
                    var line = br.nextLine();
                    if (line.startsWith(":")) {
                        session.processCommand(line);
                        break;
                    } else if (text && "EOF".equals(line)) {
                        break;
                    } else if (text && line.endsWith("EOF")) {
                        var lastLine = line.substring(0, line.indexOf("EOF"));
                        input.process(lastLine);
                        break;
                    } else if (InputType.BYTESTRING == inputType) {
                        input.processSingleLine(line);
                        break;
                    } else {
                        input.process(line);
                        if (json && input.isComplete()) {
                            break;
                        }
                    }
                }
                if (input.isComplete()) {
                    printDecodedMessage(decoder, input);
                }
                input = new Input();
            } while (session.isActive());
        }
    }

    private void printDecodedMessage(Decoder decoder, Input input) {
        final var json = inputType == InputType.JSON;
        final var text = inputType == InputType.TEXT;
        try {
            Message proto;
            var msgDescriptor = decoder.findMessageDescriptor(messageName);

            if (inputType == InputType.BYTESTRING) {
                var data = ByteString.copyFromUtf8(input.getInputText()).toByteArray();
                var jsonString = decoder.toJson(msgDescriptor, data);
                log.info("json string {}", jsonString);
                return;
            } else if (text) {
                proto = decoder.toMessageFromText(msgDescriptor, input.getInputText());
            } else if (json) {
                proto = decoder.toMessage(msgDescriptor, input.getInputText());
            } else {
                throw new IllegalStateException();
            }
            export(decoder, proto);

        } catch (HandledRuntimeException e) {
            //all handled
        }
    }

    private void export(Decoder decoder, Message proto) {
        switch (exportAs) {
            case "json":
                var decoded = decoder.toJson(proto);
                log.info("decoded message as json \n{}", decoded);
                break;
            case "bin":
                var bytes = proto.toByteArray();
                if (nonNull(output)) {
                    toOutputFile(bytes);
                } else {
                    log.info("decoded proto in binary\nBINARY_BEGIN\n{}BINARY_END", new String(bytes, Charset.forName("UTF8")));
                }
                break;
            case "bytestring":
                log.info("decoded proto as bytestring\n{}", ByteString.copyFrom(proto.toByteArray()).toString());
                break;
            case "text":
                String textVal = decoder.toText(proto);
                log.info("decoded message as text \n{}", textVal);
                break;
        }
    }

    private void toOutputFile(byte[] bytes) {
        try {
            Files.write(output.toPath(), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.info("proto exported to: {} bytes witted: {} bytes", output.getAbsolutePath(), bytes.length);
        } catch (IOException e) {
            log.error("while exporting", e);
        }
    }

    @NotNull
    private List<Path> getPaths() {
        return Stream.of(desc).map(d -> d.toPath()).collect(Collectors.toList());
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

    @Override
    public void update(String paramName, String paramValue) {
        switch (paramName) {
            case "exportAs":
                exportAs = ExportAs.of(paramValue).keyword();
                break;
        }
    }
}
