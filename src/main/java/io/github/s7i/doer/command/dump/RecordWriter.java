package io.github.s7i.doer.command.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.s7i.doer.Globals;
import io.github.s7i.doer.manifest.dump.Topic;
import java.time.Instant;
import java.util.Base64;
import java.util.Map.Entry;
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
    private Gson gson = new GsonBuilder()
          .setPrettyPrinting()
          .create();

    public String toJsonString(ConsumerRecord<String, byte[]> record) {
        var kafka = new JsonObject();
        var headers = new JsonObject();

        var json = new JsonObject();
        kafka.add("headers", headers);
        json.add("kafka", kafka);

        kafka.addProperty("offset", record.offset());
        kafka.addProperty("key", record.key());
        kafka.addProperty("topic", record.topic());
        kafka.addProperty("partition", record.partition());
        kafka.addProperty("timestamp", Instant.ofEpochMilli(record.timestamp()).toString());
        applyAdditionalKafkaProperties(kafka);

        for (var header : record.headers()) {
            headers.addProperty(header.key(), new String(header.value()));
        }
        if (specs.isShowBinary()) {
            json.addProperty("base64", Base64.getEncoder().encodeToString(record.value()));
        }
        if (specs.hasProto()) {
            var proto = protoJsonWriter.toJson(record.topic(), record.value());
            var jsProto = gson.fromJson(proto, JsonObject.class);
            jsProto.keySet().forEach(key -> json.add(key, jsProto.get(key)));
        } else if (specs.isJson()) {
            var value = gson.fromJson(new String(record.value()), JsonObject.class);
            value.keySet().forEach(key -> json.add(key, value.get(key)));
        } else {
            json.addProperty("value", new String(record.value()));
        }
        return gson.toJson(json);
    }

    protected void applyAdditionalKafkaProperties(JsonObject kafka) {
        Globals.INSTANCE.getParams().entrySet().stream()
              .filter(e -> e.getKey().equals("doer.kafka.tags"))
              .map(Entry::getValue)
              .findFirst()
              .ifPresent(tags -> {
                  var tagsArr = new JsonArray();
                  for (var tag : tags.split("\\,")) {
                      tagsArr.add(tag);
                  }
                  kafka.add("tags", tagsArr);
              });
    }
}
