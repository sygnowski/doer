package io.github.s7i.doer.session;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.session.input.InputHandler;
import io.github.s7i.doer.util.Clipboard;
import java.util.Arrays;
import lombok.Setter;

public class InteractiveSession {

    enum Cmd implements Command {
        UNDEFINED(""),
        QUIT(":quit"),
        SET(":set"),
        CLIPBOARD(":clipboard");

        Cmd(String keyword) {
            this.keyword = keyword;
        }

        final String keyword;

        @Override
        public String keyword() {
            return keyword;
        }

        static Cmd getFrom(String input) {
            return Arrays.stream(values())
                  .filter(c -> c != UNDEFINED)
                  .filter(c -> input.startsWith(c.keyword))
                  .findFirst()
                  .orElse(UNDEFINED);
        }
    }

    boolean active = true;
    @Setter
    ParamStorage storage;

    public void processCommand(String command, InputHandler inputHandler) {
        var cmd = Cmd.getFrom(command);
        switch (cmd) {
            case QUIT:
                active = false;
                break;
            case SET:
                onSetCommand(command, cmd);
                break;
            case CLIPBOARD:
                inputHandler.processOnce(Clipboard.getString());
                break;
        }
    }

    private void onSetCommand(String command, Cmd cmd) {
        var argsPart = cmd.argPart(command);
        if (!argsPart.isBlank()) {
            var args = argsPart.split("\\s");
            Arrays.stream(args)
                  .filter(s -> !s.isEmpty())
                  .map(a -> new Arg(a))
                  .forEach(this::onArg);
        }
    }

    void onArg(Arg arg) {
        if (nonNull(storage)) {
            storage.update(arg.getName(), arg.getValue());
        }
    }

    public boolean isActive() {
        return active;
    }
}
