package io.github.s7i.doer.util;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import lombok.SneakyThrows;

public class Clipboard {

    @SneakyThrows
    public static String getString() {
        return (String) Toolkit.getDefaultToolkit()
              .getSystemClipboard().getData(DataFlavor.stringFlavor);
    }
}
