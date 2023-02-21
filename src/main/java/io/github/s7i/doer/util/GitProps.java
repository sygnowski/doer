package io.github.s7i.doer.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class GitProps {

    public static final String GIT_PROPERTIES = "/git.properties";

    @Override
    public String toString() {
        var props = new Properties();
        try (var is = GitProps.class.getResourceAsStream(GIT_PROPERTIES)) {
            props.load(is);
        } catch (Exception io) {
            log.error("loading git properties", io);
        }
        var branch = props.getProperty("git.branch", "");
        var commit = props.getProperty("git.commit.id.abbrev", "");
        return String.format("%s | %s", branch, commit);
    }
}
