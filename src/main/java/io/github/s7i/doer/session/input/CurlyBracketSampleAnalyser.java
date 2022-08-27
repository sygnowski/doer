package io.github.s7i.doer.session.input;

public class CurlyBracketSampleAnalyser {

    boolean curlyBracketWasOpened;
    int openCurlyBracketCount;

    public CurlyBracketSampleAnalyser processSample(String input) {
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
        return this;
    }

    public boolean isCompletedSample() {
        return curlyBracketWasOpened && openCurlyBracketCount == 0;
    }

}
