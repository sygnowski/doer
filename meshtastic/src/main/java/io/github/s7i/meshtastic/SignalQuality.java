package io.github.s7i.meshtastic;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum SignalQuality {

    NONE,
    BAD,
    FAIR,
    GOOD;
    private static final float SNR_GOOD_THRESHOLD = -7f;
    private static final float SNR_FAIR_THRESHOLD = -15f;

    private static final int RSSI_GOOD_THRESHOLD = -115;
    private static final int RSSI_FAIR_THRESHOLD = -126;


    public static SignalQuality snr(float snr) {
        var quality = NONE;
        if (snr > SNR_GOOD_THRESHOLD) {
            quality = GOOD;
        } else if (snr > SNR_FAIR_THRESHOLD) {
            quality = FAIR;
        } else {
            quality = BAD;
        }
        return quality;
    }

    public static SignalQuality rssi(int rssi) {
        var quality = NONE;
        if (rssi > RSSI_GOOD_THRESHOLD) {
            quality = GOOD;
        } else if (rssi > RSSI_FAIR_THRESHOLD) {
            quality = FAIR;
        } else {
            quality = BAD;
        }
        return quality;
    }

    public static SignalQuality determineSignalQuality(float snr, int rssi) {
        return Stream.<Supplier<SignalQuality>>of(
                    () -> snr > SNR_GOOD_THRESHOLD && rssi > RSSI_GOOD_THRESHOLD ? GOOD : null,
                    () -> snr > SNR_GOOD_THRESHOLD && rssi > RSSI_FAIR_THRESHOLD ? FAIR : null,
                    () -> snr > SNR_FAIR_THRESHOLD && rssi > RSSI_GOOD_THRESHOLD ? FAIR : null,
                    () -> snr <= SNR_FAIR_THRESHOLD && rssi <= RSSI_FAIR_THRESHOLD ? NONE : null
              ).map(Supplier::get)
              .map(Optional::ofNullable)
              .findFirst()
              .orElseThrow().orElse(NONE);
    }
}
