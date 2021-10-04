package io.github.s7i.doer.domain.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELCondition;

@RequiredArgsConstructor
@Slf4j
public class Rule {

    final String content;

    public boolean testRule(String rawJson) {
        try {
            var jsonAsMap = new ObjectMapper().readValue(rawJson, Map.class);
            return testRule(jsonAsMap);
        } catch (Exception e) {
            log.error("rule failed", e);
            return true;
        }
    }

    public boolean testRule(Map<String, String> value) {
        var facts = new Facts();
        facts.put("value", value);
        var condition = new MVELCondition(content);
        var evaluate = condition.evaluate(facts);
        log.debug("rule {}, result {}", content, evaluate);
        return evaluate;
    }

}
