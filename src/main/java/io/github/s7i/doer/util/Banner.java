package io.github.s7i.doer.util;

import io.github.s7i.doer.Doer;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public interface Banner {

    default void printBanner() {
        Supplier<String> color = new Supplier<>() {
            List<String> rainbow = List.of(
                    "fg(129)",
                    "fg(54)",
                    "fg(21)",
                    "fg(82)",
                    "fg(226)",
                    "fg(220)",
                    "fg(196)"
            );
            int clrIdx = new Random().nextInt(rainbow.size());
            @Override
            public String get() {
                if (clrIdx >= rainbow.size()) {
                    clrIdx = 0;
                }
                return rainbow.get(clrIdx++);
            }
        };

        try (var is = Doer.class.getResourceAsStream("/banner.txt")) {
            new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .map(l -> "@|" + color.get() + " " + l +" |@")
                    .map(CommandLine.Help.Ansi.AUTO::string)
                    .forEach(System.out::println);
        } catch (Exception e) {
            Doer.console().warn("oops" , e);
        }
    }
}
