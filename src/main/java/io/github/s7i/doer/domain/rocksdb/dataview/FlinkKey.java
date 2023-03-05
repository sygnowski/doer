package io.github.s7i.doer.domain.rocksdb.dataview;

import com.google.common.io.ByteStreams;
import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.rocksdb.Key;
import io.github.s7i.doer.shade.flink.StringValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FlinkKey implements Key {

    public String readKey(byte[] data) {
        try {
            return StringValue.readString(ByteStreams.newDataInput(data));
        } catch (IOException e) {
            throw new DoerException(e);
        }
    }

    public byte[] writeKey(String data) {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            StringValue.writeString(data, ByteStreams.newDataOutput(byteArrayOutputStream));
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new DoerException(e);
        }
    }

    @Override
    public byte[] getKeyData() {
        return new byte[0];
    }
}
