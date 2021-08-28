package io.github.s7i.doer.session;

import io.github.s7i.doer.session.input.CurlyBracketSampleAnalyser;
import org.apache.commons.text.StringEscapeUtils;

public class Input {

    boolean markAsIsCompleted;
    CurlyBracketSampleAnalyser sampleAnalyser = new CurlyBracketSampleAnalyser();
    StringBuilder builder = new StringBuilder();

    public void process(String input) {
        sampleAnalyser.processSample(input);
        builder.append(input).append("\n");
    }

    public void processSingleLine(String input) {
        builder.append(input);
        markAsIsCompleted = true;
    }


    public boolean isComplete() {
        if (markAsIsCompleted) {
            return true;
        }
        return sampleAnalyser.isCompletedSample();
    }

    public String getInputText() {
        var input = builder.toString();
        input = StringEscapeUtils.unescapeJava(input);
        return input;
    }
}
