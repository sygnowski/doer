package io.github.s7i.doer.session;

public interface Command {

    String keyword();

    default String argPart(String rawCommand) {
        if (rawCommand.length() > keyword().length()) {
            return rawCommand.substring(keyword().length() + 1);
        }
        return "";
    }

}
