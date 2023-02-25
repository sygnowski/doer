package io.github.s7i.doer.pipeline.grcp;

import io.github.s7i.doer.pipeline.BlockingPipePuller;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcInboundConnection extends GrpcConnection {

    final BlockingPipePuller pipe = new BlockingPipePuller();

    public GrpcInboundConnection(String target) {
        super(target);
    }

    @Override
    public void close() throws Exception {

    }
}
