package io.github.s7i.doer.domain.output;

import static io.github.s7i.doer.Doer.console;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleOutput implements Output {

    public static final String CONSOLE = "doer://console";

    @Override
    public void open() {

    }

    @Override
    public void emit(Load load) {
        console().info("---->\n{}\n{}", load.resource, load.dataAsString());
    }

    @Override
    public void close() throws Exception {

    }
}
