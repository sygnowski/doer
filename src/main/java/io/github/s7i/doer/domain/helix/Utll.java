package io.github.s7i.doer.domain.helix;

import org.apache.helix.Criteria;
import org.apache.helix.HelixManager;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.controller.dataproviders.ResourceControllerDataProvider;
import org.apache.helix.messaging.AsyncCallback;
import org.apache.helix.messaging.handling.MessageHandler;
import org.apache.helix.messaging.handling.MessageHandlerFactory;
import org.apache.helix.model.Message;

import java.util.UUID;

public class Utll {

    public static String asKey(String resourceName, String partitionKey) {
        return resourceName + partitionKey;
    }

    @SuppressWarnings("deprecated")
    public static void sendMessage(HelixManager manager) {

        var messaging = manager.getMessagingService();

        messaging.registerMessageHandlerFactory("", new MessageHandlerFactory() {
            @Override
            public MessageHandler createHandler(Message message, NotificationContext context) {
                return null;
            }

            @Override
            public String getMessageType() {
                return null;
            }

            @Override
            public void reset() {

            }
        });

        var msg = new Message(Message.MessageType.USER_DEFINE_MSG, UUID.randomUUID().toString());
        msg.setMsgSubType("myType");
        msg.setMsgState(Message.MessageState.NEW);

        var criteria = new Criteria();
        criteria.setInstanceName("%");
        criteria.setRecipientInstanceType(InstanceType.CONTROLLER);
        criteria.setResource("");
        criteria.setPartition("");
        criteria.setSessionSpecific(true);

        var callback = new AsyncCallback() {
            @Override
            public void onTimeOut() {

            }

            @Override
            public void onReplyMessage(Message message) {

            }
        };

        messaging.sendAndWait(criteria, msg, callback, 30_000);

    }


    /**
     * https://github.com/apache/helix/blob/d9e9dcaa29357573b356c7d3f601dca612958731/helix-core/src/main/java/org/apache/helix/util/HelixUtil.java#L279
     */
    public void info(HelixManager helixManager, String clusterName) {
        var dataAccessor = helixManager.getHelixDataAccessor();

        dataAccessor.getChildNames(dataAccessor.keyBuilder().liveInstances());

        var dataProvider = new ResourceControllerDataProvider(clusterName);

        dataProvider.requireFullRefresh();
        dataProvider.refresh(dataAccessor);
        dataProvider.getLiveInstances();

    }
}
