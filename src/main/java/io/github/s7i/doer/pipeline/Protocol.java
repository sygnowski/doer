package io.github.s7i.doer.pipeline;

import io.github.s7i.doer.pipeline.proto.MetaOp;

public class Protocol {

    public interface Operation {
        MetaOp perform(MetaOp action);
    }

    public static final String OP_ADD_NEW = "add-new";
}
