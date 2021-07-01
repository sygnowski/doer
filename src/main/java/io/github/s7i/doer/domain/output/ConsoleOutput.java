package io.github.s7i.doer.domain.output;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleOutput implements Output {

    @Override
    public void open() {

    }

    @Override
    public void emit(Load load) {
        log.info("console: {}", load.dataAsString());
    }

    @Override
    public void close() throws Exception {

    }
}
