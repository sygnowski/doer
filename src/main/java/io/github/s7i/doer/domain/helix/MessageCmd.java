package io.github.s7i.doer.domain.helix;


import io.github.s7i.doer.Doer;
import io.github.s7i.doer.HandledRuntimeException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.helix.Criteria;
import org.apache.helix.InstanceType;
import org.apache.helix.model.Message;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j(topic = "doer.console")
@CommandLine.Command(name = "message", showDefaultValues = true, description = "Helix Messaging Command.")
public class MessageCmd implements Callable<Integer> {

    public static final String ALL = "%";
    @CommandLine.Option(names = "-n", defaultValue = "messenger")
    String name;
    @CommandLine.Option(names = "-s", defaultValue = "localhost:2181", scope = CommandLine.ScopeType.INHERIT)
    String server;
    @CommandLine.Option(names = "-c", required = true, scope = CommandLine.ScopeType.INHERIT)
    String clusterName;
    @CommandLine.Option(names = "--from", required = true)
    String from;
    @CommandLine.Option(names = "--target", required = true)
    String target;
    @CommandLine.Option(names = "--content", required = true)
    String content;

    @CommandLine.Option(names = "--recipient", defaultValue = ALL)
    String recipientName;

    @CommandLine.Option(names = "--expiry", defaultValue = "-1", description = "Expiry in seconds.")
    int expiry;

    @CommandLine.Option(names = "--dataSource", defaultValue = "LIVEINSTANCES", description = "Options: ${COMPLETION-CANDIDATES}")
    Criteria.DataSource dataSource;

    @CommandLine.Option(names = "--instanceType", defaultValue = "PARTICIPANT", description = "Options: ${COMPLETION-CANDIDATES}")
    InstanceType instanceType;


    public static class Messenger extends HelixMember {
        final MessageCmd cmd;

        public Messenger(MessageCmd cmd) {
            super(cmd.name, cmd.clusterName, cmd.server);
            this.cmd = cmd;
        }

        public boolean sendMessage() throws Exception {
            try {
                log.info("start sending message...");

                connect(InstanceType.ADMINISTRATOR);

                final var criteria = new Criteria();

                criteria.setSessionSpecific(false);
                criteria.setInstanceName(cmd.recipientName);
                criteria.setSelfExcluded(true);
                criteria.setRecipientInstanceType(cmd.instanceType);
                criteria.setDataSource(cmd.dataSource);
                criteria.setResource(ALL);
                criteria.setPartition(ALL);

                final var message = new Message(Message.MessageType.USER_DEFINE_MSG, UUID.randomUUID().toString());
                message.setMsgState(Message.MessageState.NEW);
                message.setTgtSessionId("*");

                message.getRecord().setSimpleField("DOER_CREATOR", "YES");
                message.getRecord().setRawPayload(cmd.content.getBytes(StandardCharsets.UTF_8));

                if (cmd.expiry >= 0) {
                    message.setExpiryPeriod(cmd.expiry * 1000L);
                }

                message.setSrcName(cmd.from);
                message.setTgtName(cmd.target);

                int sent = helixManager.getMessagingService().send(criteria, message);
                log.info("messages sent: {}", sent);
                return sent > 0;
            } catch (Exception e) {
                log.error("oops", e);
                throw new HandledRuntimeException(e);
            } finally {
                helixManager.disconnect();
            }
        }
    }


    @SneakyThrows
    @Override
    public Integer call() {
        return new Messenger(this).sendMessage() ? 0 : Doer.EC_ERROR;
    }
}
