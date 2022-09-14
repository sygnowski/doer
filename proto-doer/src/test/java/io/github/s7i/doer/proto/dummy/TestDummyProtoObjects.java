package io.github.s7i.doer.proto.dummy;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Disabled
public class TestDummyProtoObjects {


    @Test
    public void test01() {
        var anyAlpha = Any.pack(StringValue.of("this is alpha"));
        var anyBeta = Any.pack(StringValue.of("this is beta"));
        var symbol = GreekSymbols.newBuilder()
                .setAlpha(anyAlpha)
                .setBeta(anyBeta)
                .build();

        var wrapper =  GreekSymbols.newBuilder()
                .setAlpha(Any.pack(UserInfo.newBuilder()
                        .setName("mario")
                        .setAge(33)
                        .build()))
                .setZeta(Any.pack(symbol))
                .build();

        System.out.println(symbol);
        System.out.println(wrapper);
        System.out.println(Base64.getEncoder().encodeToString(wrapper.toByteArray()));

    }

    @Test
    public void test02() throws Exception {

        var input = "\n\005mario\020!";
        var bs = ByteString.copyFrom(input.getBytes(StandardCharsets.UTF_8));
        var data = bs.toByteArray();

        var userInfo = UserInfo.newBuilder()
                .setName("mario")
                .setAge(33)
                .build();
        var packed = Any.pack(userInfo);
        var data2 = packed.getValue().toString();
        packed.unpack(UserInfo.class);


        var ui = UserInfo.parseFrom(input.getBytes(StandardCharsets.UTF_8));
        System.out.println(ui);
        Assertions.assertEquals(userInfo, ui);

    }
}
