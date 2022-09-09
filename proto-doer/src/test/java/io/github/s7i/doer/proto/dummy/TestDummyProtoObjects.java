package io.github.s7i.doer.proto.dummy;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;

public class TestDummyProtoObjects {


    @Test
    public void test01() {
        //TODO: Any:pack no such method in proto-lite version of code.
        var al = Any.pack(StringValue.of("this is alpha"));
        var symbol = GreekSymbols.newBuilder()
                .setAlpha(al)
                .build();

        System.out.println(symbol);

    }
}
