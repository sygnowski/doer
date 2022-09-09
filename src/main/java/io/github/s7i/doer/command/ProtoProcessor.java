package io.github.s7i.doer.command;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.Doer;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.HandledRuntimeException;
import io.github.s7i.doer.proto.Decoder;
import io.github.s7i.doer.session.Input;
import io.github.s7i.doer.session.InteractiveSession;
import io.github.s7i.doer.util.PropertyResolver;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(name = "proto", description = "Protocol buffers decoder/encoder.")
public class ProtoProcessor implements Callable<Integer>, ConsoleLog {

    public static final String EOF = "EOF";
    public static final String DOER_PROMPT = "doer > ";

    enum InputType {
        TEXT, JSON, BYTESTRING
    }

    enum ExportAs {
        BIN, TEXT, JSON, BYTESTRING, BASE64;

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

    @Option(names = "-d", arity = "1..*", description = "Proto buffers descriptors files.")
    private File[] desc;

    @Option(names = "-i", description = "Interactive mode.")
    private boolean interactive;

    @Option(names = {"-t", "--inputType"}, defaultValue = "json")
    private InputType inputType;

    @Option(names = "-m")
    private String messageName;

    @Option(names = "-f")
    private File protoData;

    @Option(names = {"-e", "--exportAs"}, defaultValue = "json")
    private ExportAs exportAs;

    @Option(names = {"-o", "--output"})
    private File output;

    @Option(names = {"--base64"}, description = "Base64 input.")
    private String base64;

    @Override
    public Integer call() {
        try {
            if (interactive) {
                processInteractive();
            } else {
                if (isNull(messageName) || isNull(desc) || desc.length == 0) {
                    new CommandLine(ProtoProcessor.class).usage(System.out);
                    return Doer.EC_INVALID_USAGE;
                }
                switch (exportAs) {
                    case BIN:
                        if (nonNull(output)) {
                            toOutputFile(encodeProto());
                        } else {
                            System.out.write(encodeProto());
                        }
                        break;
                    case BASE64:
                        if (nonNull(output)) {
                            toOutputFile(Base64.getEncoder().encode(encodeProto()));
                        } else {
                            System.out.println(Base64.getEncoder().encodeToString(encodeProto()));
                        }
                        break;
                    default:
                        decodeProto();
                        break;
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("running command error", e);
            return Doer.EC_ERROR;
        }
    }


    @SneakyThrows
    private byte[] readProtoInput() {
        if (nonNull(protoData) && protoData.exists()) {
            final var path = protoData.toPath();
            final var fileName = path.getFileName().toString();
            if (fileName.endsWith(".json") || fileName.endsWith(".txt")) {
                var textInput = new String(Files.readAllBytes(path));
                return new PropertyResolver().resolve(textInput).getBytes(StandardCharsets.UTF_8);
            }
            return Files.readAllBytes(path);
        } else {
            throw new DoerException("invalid input");
        }
    }

    private void decodeProto() throws IOException {
        var data = isNull(base64) || isBlank(base64)
              ? readProtoInput()
              : Base64.getDecoder().decode(base64);

        var paths = getPaths();
        var decoded = decode(paths, messageName, data);
        info("Decoded proto:\n {}", decoded);
    }

    private byte[] encodeProto() {
        var decoder = new Decoder().loadDescriptors(getPaths());
        var descriptor = decoder.findMessageDescriptor(messageName);
        var textData = new String(readProtoInput());
        try {
            return decoder.toMessage(descriptor, textData).toByteArray();
        } catch (HandledRuntimeException e) {
            return decoder.toMessageFromText(descriptor, textData).toByteArray();
        }
    }

    private void processInteractive() throws IOException {
        final var json = inputType == InputType.JSON;
        final var text = inputType == InputType.TEXT;
        var decoder = new Decoder();
        decoder.loadDescriptors(getPaths());

        var input = new Input();

        var session = new InteractiveSession();
        session.setStorage(this::updateParameters);
        session.setInputHandler(input);

        try (var br = new Scanner(new InputStreamReader(System.in))) {
            do {
                printBanner();

                while (br.hasNextLine()) {
                    var line = br.nextLine();
                    if (line.startsWith(":")) {
                        session.processCommand(line);
                        break;
                    } else if (text && EOF.equals(line)) {
                        break;
                    } else if (text && line.endsWith(EOF)) {
                        var lastLine = line.substring(0, line.indexOf(EOF));
                        input.process(lastLine);
                        break;
                    } else if (InputType.BYTESTRING == inputType) {
                        input.processOnce(line);
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

    private void printBanner() {
        var pw = new PrintWriter(System.out, true);

        pw.printf("\nPlease provide '%s' input.\n", inputType);
        pw.printf("Input will be transformed to '%s' of '%s' proto type\n\n", exportAs, messageName);

        pw.print(DOER_PROMPT);
        pw.flush();

    }

    private void printDecodedMessage(Decoder decoder, Input input) {
        try {
            Message proto;
            var msgDescriptor = decoder.findMessageDescriptor(messageName);

            switch (inputType) {
                case BYTESTRING:
                    var data = ByteString.copyFromUtf8(input.getInputText()).toByteArray();
                    var jsonString = decoder.toJson(msgDescriptor, data);
                    info("Result \n{}", jsonString);
                    return;
                case TEXT:
                    proto = decoder.toMessageFromText(msgDescriptor, input.getInputText());
                    break;
                case JSON:
                    proto = decoder.toMessage(msgDescriptor, input.getInputText());
                    break;
                default:
                    throw new IllegalStateException();

            }
            export(decoder, proto);

        } catch (HandledRuntimeException e) {
            //all handled
        }
    }

    private void export(Decoder decoder, Message proto) {
        switch (exportAs) {
            case JSON:
                var decoded = decoder.toJson(proto);
                info("decoded message as json \n{}", decoded);
                break;
            case BIN:
                var bytes = proto.toByteArray();
                if (nonNull(output)) {
                    toOutputFile(bytes);
                } else {
                    info("decoded proto in binary\nBINARY_BEGIN\n{}BINARY_END", new String(bytes, StandardCharsets.UTF_8));
                }
                break;
            case BYTESTRING:
                info("decoded proto as bytestring\n{}", ByteString.copyFrom(proto.toByteArray()).toString());
                break;
            case TEXT:
                String textVal = decoder.toText(proto);
                info("decoded message as text \n{}", textVal);
                break;
            case BASE64:
                info("decoded version as base64: {}", Base64.getEncoder().encode(proto.toByteArray()));
                break;
        }
    }

    private void toOutputFile(byte[] bytes) {
        try {
            Files.write(output.toPath(), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            info("proto exported to: {} bytes witted: {} bytes", output.getAbsolutePath(), bytes.length);
        } catch (IOException e) {
            log.error("while exporting", e);
        }
    }

    @NotNull
    private List<Path> getPaths() {
        return Stream.of(desc).map(File::toPath).collect(Collectors.toList());
    }

    public Message decode(List<Path> descriptorPaths, String messageName, byte[] data) {
        var decoder = new Decoder();
        decoder.loadDescriptors(descriptorPaths);
        var descriptor = decoder.findMessageDescriptor(messageName);
        return decoder.toMessage(descriptor, data);
    }

    public void updateParameters(String paramName, String paramValue) {
        switch (paramName) {
            case "ea":
            case "exportAs":
                exportAs = ExportAs.of(paramValue);
                break;
            case "mt":
            case "messageType":
                messageName = paramValue;
                break;
            case "it":
            case "inputType":
                try {
                    inputType = InputType.valueOf(paramValue.toUpperCase());
                } catch (Exception e) {
                    log.warn("can't change input type to: {}", paramValue);
                }
                break;
        }
    }
}
