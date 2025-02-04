package io.github.s7i.meshtastic;

import com.geeksville.mesh.MeshProtos.ToRadio;
import com.google.protobuf.Message;

public enum Proto {
    INSTANCE;

    public Message getConfiguration(int configId) {
        return ToRadio.newBuilder()
              .setWantConfigId(configId)
              .build();
    }
}
