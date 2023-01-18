package io.github.s7i.doer.domain.helix;

import java.util.UUID;
import org.apache.helix.Criteria;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.messaging.AsyncCallback;
import org.apache.helix.messaging.handling.MessageHandler;
import org.apache.helix.messaging.handling.MessageHandlerFactory;
import org.apache.helix.model.Message;
import org.apache.helix.model.Message.MessageState;
import org.apache.helix.model.Message.MessageType;
import org.apache.helix.zookeeper.datamodel.ZNRecord;

public class Participant extends HelixMember {

    public Participant(String instanceName, String clusterName, String server) throws Exception {
        super(instanceName, clusterName, server);

        connect(InstanceType.PARTICIPANT);
    }

    @Override
    protected void onBefore(HelixManager manager) {
        var record = new ZNRecord(UUID.randomUUID().toString());
        record.setSimpleField("X_TEST", "test of simple field");

        manager.setLiveInstanceInfoProvider(() -> record);
    }

    @Override
    protected void onAfter(HelixManager manager) {
//        var messaging = manager.getMessagingService();
//
//        messaging.registerMessageHandlerFactory("", new MessageHandlerFactory() {
//            @Override
//            public MessageHandler createHandler(Message message, NotificationContext context) {
//                return null;
//            }
//
//            @Override
//            public String getMessageType() {
//                return null;
//            }
//
//            @Override
//            public void reset() {
//
//            }
//        });
//
//        var msg = new Message(MessageType.USER_DEFINE_MSG, UUID.randomUUID().toString());
//        msg.setMsgSubType("myType");
//        msg.setMsgState(MessageState.NEW);
//
//        var criteria = new Criteria();
//        criteria.setInstanceName("%");
//        criteria.setRecipientInstanceType(InstanceType.CONTROLLER);
//        criteria.setResource("");
//        criteria.setPartition("");
//        criteria.setSessionSpecific(true);
//
//        var callback = new AsyncCallback() {
//            @Override
//            public void onTimeOut() {
//
//            }
//
//            @Override
//            public void onReplyMessage(Message message) {
//
//            }
//        };
//
//        messaging.sendAndWait(criteria, msg, callback, 30_000);
    }
}
