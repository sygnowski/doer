package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MeshProtos.FromRadio;
import com.geeksville.mesh.MeshProtos.FromRadio.PayloadVariantCase;
import com.geeksville.mesh.MeshProtos.NeighborInfo;
import com.geeksville.mesh.MeshProtos.NodeInfo;
import com.geeksville.mesh.MeshProtos.Position;
import com.geeksville.mesh.MeshProtos.Routing;
import com.geeksville.mesh.MeshProtos.ToRadio;
import com.geeksville.mesh.TelemetryProtos;
import com.geeksville.mesh.TelemetryProtos.Telemetry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import java.util.Map;

/**
 * Gateway class to Meshtastic Protobuf classes.
 */
public enum Proto {
    INSTANCE;

    private final Printer printer = JsonFormat.printer().usingTypeRegistry(TypeRegistry.newBuilder()
          .add(MeshProtos.getDescriptor().getMessageTypes())
          .add(TelemetryProtos.getDescriptor().getMessageTypes())
          .build());
    private final Gson gson = new Gson();

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

    public String asJsonTextFromRadio(byte[] data) {

        try {
            var fromRadio = FromRadio.parseFrom(data);

            var root = gson.fromJson(printer.print(fromRadio), JsonObject.class);

            if (fromRadio.getPayloadVariantCase() == PayloadVariantCase.PACKET) {

                var packet = fromRadio.getPacket();
                var decoded = packet.getDecoded();
                try {
                    Message unroll = switch (decoded.getPortnum()) {
                        case POSITION_APP -> Position.parseFrom(decoded.getPayload());
                        case TELEMETRY_APP -> Telemetry.parseFrom(decoded.getPayload());
                        case NEIGHBORINFO_APP -> NeighborInfo.parseFrom(decoded.getPayload());
                        case ROUTING_APP -> Routing.parseFrom(decoded.getPayload());
                        case NODEINFO_APP -> NodeInfo.parseFrom(decoded.getPayload());
                        default -> null;
                    };
                    if (unroll != null) {
                        var json = new JsonObject();

                        root.add("ext", json);

                        var jsProto = gson.fromJson(printer.print(unroll), JsonObject.class);
                        jsProto.keySet().forEach(key -> json.add(key, jsProto.get(key)));
                    }
                } catch (InvalidProtocolBufferException e) {
                    root.addProperty("doer.error", e.getMessage());

                }

            }
            return gson.toJson(root);

        } catch (InvalidProtocolBufferException e) {
            return gson.toJson(Map.of("error", "Meshtastic :: FromRadio :: Unable to decode: " + e.getMessage()));
        }
    }
}
