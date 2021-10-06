package io.github.s7i.doer.command.dump;

import static io.github.s7i.doer.util.Utils.hasAnyValue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.s7i.doer.config.Dump.Topic;
import java.time.Instant;
import java.util.Base64;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecordWriter {

    final Topic specs;
    @Getter
    final ProtoJsonWriter protoJsonWriter;

    public String toJsonString(ConsumerRecord<String, byte[]> record) {
        var kafka = new JsonObject();
        var headers = new JsonObject();

        var json = new JsonObject();
        kafka.add("headers", headers);
        json.add("kafka", kafka);

        kafka.addProperty("offset", record.offset());
        kafka.addProperty("key", record.key());
        kafka.addProperty("topic", record.topic());
        kafka.addProperty("timestamp", Instant.ofEpochMilli(record.timestamp()).toString());

        for (var header : record.headers()) {
            headers.addProperty(header.key(), new String(header.value()));
        }
        if (specs.isShowBinary()) {
            json.addProperty("base64", Base64.getEncoder().encodeToString(record.value()));
        }

        var gson = new GsonBuilder()
              .setPrettyPrinting()
              .create();

        if (hasAnyValue(specs.getValue().getProtoMessage())) {
            var proto = protoJsonWriter.toJson(record.topic(), record.value());
            var jsProto = gson.fromJson(proto, JsonObject.class);
            jsProto.keySet().forEach(key -> json.add(key, jsProto.get(key)));
        } else {
            json.addProperty("value", new String(record.value()));
        }
        return gson.toJson(json);
    }
}
