package io.github.s7i.doer.domain.helix;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.HelixManager;
import org.apache.helix.NotificationContext;
import org.apache.helix.messaging.handling.HelixTaskResult;
import org.apache.helix.messaging.handling.MessageHandler;
import org.apache.helix.messaging.handling.MultiTypeMessageHandlerFactory;
import org.apache.helix.model.Message;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Slf4j(topic = "doer.console")
public class SimpleMessageHandler extends MessageHandler {

    @RequiredArgsConstructor
    public static class Factory implements MultiTypeMessageHandlerFactory {

        public static final String MSG_NAME;

        static {
            MSG_NAME = Message.MessageType.USER_DEFINE_MSG.name();
        }

        final EventLogger eventLogger;

        @Override
        public List<String> getMessageTypes() {
            return List.of(MSG_NAME);
        }

        @Override
        public MessageHandler createHandler(Message message, NotificationContext context) {
            return new SimpleMessageHandler(eventLogger, message, context);
        }

        @Override
        public void reset() {

        }
    }

    public static void register(HelixManager manager, EventLogger eventLogger) {
        requireNonNull(manager);
        requireNonNull(eventLogger);
        manager.getMessagingService().registerMessageHandlerFactory(Factory.MSG_NAME, new Factory(eventLogger));
    }

    protected final EventLogger eventLogger;

    public SimpleMessageHandler(EventLogger eventLogger, Message message, NotificationContext context) {
        super(message, context);
        this.eventLogger = eventLogger;
    }

    @Override
    public HelixTaskResult handleMessage() throws InterruptedException {

        eventLogger.report(new EventLogger.Event() {
            {
                attributes.putAll(Map.of(
                        "user_defined_message", true,
                        "type", _notificationContext.getType().name(),
                        "changeType", _notificationContext.getChangeType().name(),
                        "message", getMessage()));
            }
        });

        HelixTaskResult helixTaskResult = new HelixTaskResult();
        helixTaskResult.setSuccess(true);
        return helixTaskResult;
    }

    @Override
    public void onError(Exception e, ErrorCode code, ErrorType type) {
        log.error("handler error: code {}, type: {}", code, type, e);
    }
}
