package io.github.s7i.doer.session;

import static java.util.Objects.nonNull;

import java.util.Arrays;
import lombok.Setter;

public class InteractiveSession {

    enum Cmd {
        UNDEFINED(""),
        QUIT(":quit"),
        SET(":set");

        Cmd(String keyword) {
            this.keyword = keyword;
        }

        final String keyword;

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

    public void processCommand(String command) {
        var cmd = Cmd.getFrom(command);
        switch (cmd) {
            case QUIT:
                active = false;
                break;
            case SET:
                command = command.substring(Cmd.SET.keyword.length() + 1);
                var args = command.split("\\s");
                Arrays.stream(args)
                      .filter(s -> !s.isEmpty())
                      .map(a -> new Arg(a))
                      .forEach(this::onArg);
                break;
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
