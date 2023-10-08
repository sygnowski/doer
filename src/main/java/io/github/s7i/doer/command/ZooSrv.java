package io.github.s7i.doer.command;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

@CommandLine.Command(name = "zoosrv", description = "Local Zookeeper Server", showDefaultValues = true)
@Slf4j(topic = "doer.console")
public class ZooSrv implements Callable<Integer> {

    @CommandLine.Option(names = "--port", defaultValue = "2181")
    Integer port;
    @CommandLine.Option(names = "--work-dir", defaultValue = "./zoosrv")
    Path workdir;
    @CommandLine.Option(names = "--tickTime", defaultValue = "2000")
    private Integer tickTime;
    @CommandLine.Option(names = "--max-connections", defaultValue = "0", description = "0 means no limit")
    private Integer maxClientConnections;
    @CommandLine.Option(names = "--min-session-timeout", defaultValue = "-1" , description = "-1 means tickTime * 2")
    private Integer minSessionTimeout;
    private CountDownLatch lock;

    @CommandLine.Option(names = "--help", usageHelp = true)
    boolean help;


    @Override
    public Integer call() throws Exception {

        log.info("Running Local Zookeeper Server on port: {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown"));
        lock = new CountDownLatch(1);

        Path dataDir = workdir.resolve("data");
        Path logDir = workdir.resolve("log");

        Files.createDirectories(dataDir);
        Files.createDirectories(logDir);

        var zk = new ZooKeeperServer(dataDir.toFile(), logDir.toFile(), tickTime);
        zk.setMinSessionTimeout(minSessionTimeout);
        var nioFactory = new NIOServerCnxnFactory();
        nioFactory.configure(new InetSocketAddress(port), maxClientConnections);
        nioFactory.startup(zk);

        log.info("Zookeeper server running...");
        lock.await();

        log.info("Zookeeper server stopping...");
        nioFactory.shutdown();

        return 0;
    }

    void shutdown() {
        lock.countDown();
    }
}
