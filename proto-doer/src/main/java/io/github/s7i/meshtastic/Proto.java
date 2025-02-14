package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos.FromRadio;
import com.geeksville.mesh.MeshProtos.ToRadio;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Gateway class to Meshtastic Protobuf classes.
 */
public enum Proto {
    INSTANCE;

    public interface ToText {

        String print(byte[] data);
    }

    public record FromRadioMeta(ToText toText, boolean interesting) {

    }

    public Message getConfiguration(int configId) {
        return ToRadio.newBuilder()
              .setWantConfigId(configId)
              .build();
    }

    String printFromRadio(byte[] data) {
        try {
            return FromRadio.parseFrom(data).toString();
        } catch (InvalidProtocolBufferException e) {
            return "Meshtastic :: FromRadio :: Unable to decode: " + e.getMessage();
        }
    }

    public FromRadioMeta fromRadioMeta(byte[] data) throws InvalidProtocolBufferException {
        var fromRadio = FromRadio.parseFrom(data);

        return new FromRadioMeta(this::printFromRadio,
              switch (fromRadio.getPayloadVariantCase()) {
                  case QUEUESTATUS, CONFIG_COMPLETE_ID -> false;
                  default -> true;
              });
    }
}
