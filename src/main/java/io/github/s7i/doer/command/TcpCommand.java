package io.github.s7i.doer.command;

import io.github.s7i.doer.DoerException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "tcp")
public class TcpCommand extends Command {

//    public record Opt(
//          @Option(names = "--host")
//          String host) {
//
//    }
//
//    @Mixin
//    Opt opt;


    @Parameters(arity = "1..*")
    String[] args;



    int START1 = 0x94;
    int START2 = 0xC3;
    int HEADER_LEN = 4;
    int MAX_TO_FROM_RADIO_SIZE = 512;

    @Override
    public void onExecuteCommand() {

        try {

            var buff = new byte[512];

            try (var socket = new Socket(InetAddress.getByName(args[0]), Integer.parseInt(args[1]))) {
                var is = socket.getInputStream();

                var os = socket.getOutputStream();

//                var configId = new Random().nextInt();
//                var wantCfg = Proto.INSTANCE.getConfiguration(configId).toByteArray();
//
//                os.write(wantCfg);


                int c = is.read();

                TimeUnit.SECONDS.sleep(10);
            }


        } catch (Exception e) {
            throw new DoerException(e);
        }
    }
}
