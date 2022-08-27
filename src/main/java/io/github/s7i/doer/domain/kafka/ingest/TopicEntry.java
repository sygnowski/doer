package io.github.s7i.doer.domain.kafka.ingest;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TopicEntry {

    List<Header> headers = new ArrayList<>(0);
    final String key;
    final byte[] data;
}
