package io.github.s7i.doer.session;

public class Input {

    boolean curlyBracketWasOpened;
    int openCurlyBracketCount;
    boolean markAsIsCompleted;
    StringBuilder builder = new StringBuilder();

    public void process(String input) {
        processCurlyBracket(input);
        builder.append(input).append("\n");
    }

    public void processSingleLine(String input) {
        builder.append(input);
        markAsIsCompleted = true;
    }

    public void processCurlyBracket(String input) {
        for (char chr : input.toCharArray()) {
            switch (chr) {
                case '{':
                    openCurlyBracketCount++;
                    if (!curlyBracketWasOpened) {
                        curlyBracketWasOpened = true;
                    }
                    break;
                case '}':
                    if (openCurlyBracketCount > 0) {
                        openCurlyBracketCount--;
                    } else {
                        throw new IllegalStateException("invalid input");
                    }
                    break;

            }
        }
    }

    public boolean isComplete() {
        if (markAsIsCompleted) {
            return true;
        }
        return curlyBracketWasOpened && openCurlyBracketCount == 0;
    }

    public boolean isOpen() {
        return openCurlyBracketCount > 0;
    }

    public String getInputText() {
        return builder.toString();
    }
}
