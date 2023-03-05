package io.github.s7i.doer.domain.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.s7i.doer.util.Utils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.Message;

import java.util.Map;

@Slf4j(topic = "doer.console")
public class SwitchStateLogger {
    protected final ObjectMapper objectMapper = Utils.preetyObjectMapper();

    @SneakyThrows
    void logSwitchState(Message msg, NotificationContext context) {
        var changeType = context.getChangeType();
        var type = context.getType();
        var event = Map.of(
                "type", type.name(),
                "changeType", changeType.name(),
                "msg", msg
        );
        log.info("switch-state: {}\n", objectMapper.writeValueAsString(event));
    }
}
