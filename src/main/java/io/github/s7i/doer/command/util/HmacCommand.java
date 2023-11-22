package io.github.s7i.doer.command.util;

import io.github.s7i.doer.command.Command;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import picocli.CommandLine;

import java.util.Base64;

import static java.util.Objects.requireNonNull;

@CommandLine.Command(
        name = "hmac",
        description = "Hashed Message Authentication Code (HMAC)."
)
@Slf4j(topic = "doer.console")
public class HmacCommand extends Command
{

    public static final String ALG_HMAC_SHA256 = "HmacSHA256";

    @CommandLine.Option(names = "-a", description = "HMAC Algorithm. (default: HmacSHA256)", defaultValue = ALG_HMAC_SHA256)
    String alg = ALG_HMAC_SHA256;



    @CommandLine.Option(names = "-k", required = true)
    String key;

    @CommandLine.Option(names = "--keyBase64")
    boolean keyBase64;

    @CommandLine.Option(names = {"-d", "--data"}, required = true)
    String data;


    byte[] getKeyBytes() {
        requireNonNull(key, "key");
        if (keyBase64) {
            return Base64.getDecoder().decode(key);
        }
        return key.getBytes();
    }

    @Override
    public void onExecuteCommand() {

        requireNonNull(data, "data");

        var mac = new HmacUtils(alg, getKeyBytes());
        var hmacHex = mac.hmacHex(data);

        log.info("HMAC HEX: {}", hmacHex);

    }
}
