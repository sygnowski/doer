package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.MeshProtos.Constants;
import com.geeksville.mesh.MeshProtos.Data;
import com.geeksville.mesh.MeshProtos.FromRadio;
import com.geeksville.mesh.MeshProtos.FromRadio.PayloadVariantCase;
import com.geeksville.mesh.MeshProtos.MeshPacket;
import com.geeksville.mesh.MeshProtos.NeighborInfo;
import com.geeksville.mesh.MeshProtos.Position;
import com.geeksville.mesh.MeshProtos.RouteDiscovery;
import com.geeksville.mesh.MeshProtos.Routing;
import com.geeksville.mesh.MeshProtos.ToRadio;
import com.geeksville.mesh.MeshProtos.User;
import com.geeksville.mesh.Portnums.PortNum;
import com.geeksville.mesh.TelemetryProtos;
import com.geeksville.mesh.TelemetryProtos.Telemetry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import io.github.s7i.meshtastic.proto.Info;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Gateway class to Meshtastic Protobuf classes.
 */
public enum Proto {
    INSTANCE;
    public static final String GOSN_PRETTY = "gosn.pretty";
    public static final String VERSION = Info.VERSION;

    private final Printer printer = JsonFormat.printer().usingTypeRegistry(TypeRegistry.newBuilder()
          .add(MeshProtos.getDescriptor().getMessageTypes())
          .add(TelemetryProtos.getDescriptor().getMessageTypes())
          .build());

    private final PacketIdGenerator packetIdGenerator = new PacketIdGenerator();
    private final Gson gson = Stream.of(Boolean.getBoolean(GOSN_PRETTY))
          .map(pretty ->
                pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson()
          ).findAny()
          .orElseThrow();

    private final Map<Long, String> nodeNameMap = new ConcurrentHashMap<>();

    private final Double[] myLocation = Optional.ofNullable(System.getenv("MY_LOC"))
          .map(myLoc -> {
              try {
                  var myLatLLong = myLoc.split("\\:");
                  var myLat = Double.parseDouble(myLatLLong[0]);
                  var myLong = Double.parseDouble(myLatLLong[1]);
                  return new Double[]{myLat, myLong};
              } catch (Exception e) {
                  throw new IllegalStateException("Invalid location, use:MY_LOC=xx.xxx:xx.xxx", e);
              }
          }).orElse(new Double[0]);

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

    public String asJsonTextPacket(byte[] data) {

        try {
            var packet = MeshPacket.parseFrom(data);

            var root = new JsonObject();
            var jsPacketEnvelop = new JsonObject();
            root.add("packet", jsPacketEnvelop);
            jsonifyPacket(packet, jsPacketEnvelop);

            return gson.toJson(root);

        } catch (InvalidProtocolBufferException e) {
            return gson.toJson(Map.of("error", "Meshtastic :: MeshPacket :: Unable to decode: " + e.getMessage()));
        }
    }

    public String asJsonTextFromRadio(byte[] data) {

        try {
            var fromRadio = FromRadio.parseFrom(data);

            var root = gson.fromJson(printer.print(fromRadio), JsonObject.class);

            if (fromRadio.getPayloadVariantCase() == PayloadVariantCase.PACKET) {
                jsonifyPacket(fromRadio.getPacket(), root);

            } else if (fromRadio.getPayloadVariantCase() == PayloadVariantCase.NODE_INFO) {
                var nodeInfo = fromRadio.getNodeInfo();

                long id = nodeInfo.getNum() & 0xffffffffL;

                nodeNameMap.put(id, nodeInfo.getUser().getShortName() + " / " + nodeInfo.getUser().getLongName());
            }
            return gson.toJson(root);

        } catch (InvalidProtocolBufferException e) {
            return gson.toJson(Map.of("error", "Meshtastic :: FromRadio :: Unable to decode: " + e.getMessage()));
        }
    }

    private void jsonifyPacket(MeshPacket packet, JsonObject root) {
        var extJson = new JsonObject();

        root.add("ext", extJson);

        Optional.ofNullable(nodeNameMap.get(packet.getFrom() & 0xffffffffL)).ifPresent(name -> {
            extJson.addProperty("nodeFullName", name);
        });

        var sec = packet.getRxTime();
        if (sec > 0) {
            var rxTime = Instant.ofEpochSecond(sec)
                  .atZone(ZoneOffset.systemDefault())
                  .toLocalDateTime();

            extJson.addProperty("rxTimestamp", rxTime.toString());
        }
        var signalQuality = new JsonObject();
        signalQuality.addProperty("rssi", SignalQuality.rssi(packet.getRxRssi()).toString());
        signalQuality.addProperty("snr", SignalQuality.snr(packet.getRxSnr()).toString());
        signalQuality.addProperty("signal", SignalQuality.determineSignalQuality(packet.getRxSnr(), packet.getRxRssi()).toString());
        extJson.add("signalQuality", signalQuality);

        if (!packet.hasDecoded()) {
            return;
        }

        var decoded = packet.getDecoded();
        try {
            var unroll = switch (decoded.getPortnum()) {
                case POSITION_APP -> {
                    var pos = Position.parseFrom(decoded.getPayload());
                    if (myLocation.length == 2) {
                        var myLat = myLocation[0];
                        var myLong = myLocation[1];

                        var posLat = pos.getLatitudeI() * 1e-7;
                        var posLong = pos.getLongitudeI() * 1e-7;

                        var kmUnit = GeoUtils.latLongToMeter(myLat, myLong, posLat, posLong) / 1000;

                        extJson.addProperty("distance", String.format("%.1f km", kmUnit));
                    }
                    yield pos;
                }
                case TELEMETRY_APP -> Telemetry.parseFrom(decoded.getPayload());
                case NEIGHBORINFO_APP -> NeighborInfo.parseFrom(decoded.getPayload());
                case TRACEROUTE_APP -> RouteDiscovery.parseFrom(decoded.getPayload());
                case ROUTING_APP -> Routing.parseFrom(decoded.getPayload());
                case NODEINFO_APP -> User.parseFrom(decoded.getPayload());
                case TEXT_MESSAGE_APP -> {
                    extJson.addProperty("textMessage", decoded.getPayload().toStringUtf8());
                    yield null;
                }
                default -> {
                    try {
                        var msg = UnknownFieldSet.parseFrom(decoded.getPayload());
                        extJson.addProperty("other", msg.toString());
                    } catch (InvalidProtocolBufferException e) {
                        //expected
                    }
                    yield null;
                }
            };
            if (unroll != null) {
                var unrollJson = gson.fromJson(printer.print(unroll), JsonObject.class);
                unrollJson.entrySet().forEach(key -> extJson.add(key.getKey(), key.getValue()));
            }
        } catch (InvalidProtocolBufferException e) {
            root.addProperty("doer.error", "invalid proto");
        }
    }

    public Message textMessage(Supplier<Integer> from, Supplier<Integer> to, String message) {
        var payload = ByteString.copyFromUtf8(message);

        if (payload.size() > Constants.DATA_PAYLOAD_LEN_VALUE) {
            throw new IllegalStateException("payload too big");
        }

        return ToRadio.newBuilder()
              .setPacket(MeshPacket.newBuilder()
                    .setId(packetIdGenerator.generatePacketId())
                    .setFrom(from.get())
                    .setTo(to.get())
                    .setRxTime((int) Instant.now().getEpochSecond())
                    .setChannel(1)
                    .setDecoded(Data.newBuilder()
                          .setPayload(payload)
                          .setPortnum(PortNum.TEXT_MESSAGE_APP)))
              .build();
    }
}
