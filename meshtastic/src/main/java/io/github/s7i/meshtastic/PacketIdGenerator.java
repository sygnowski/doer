package io.github.s7i.meshtastic;

import java.util.Random;

public class PacketIdGenerator {


    private volatile long currentPacketId = new Random(System.currentTimeMillis()).nextLong();

    /**
     * Generate a unique packet ID (if we know enough to do so - otherwise return 0 so the device will do it)
     */
    public synchronized int generatePacketId() {
        long numPacketIds =
              ((1L << 32) - 1); // A mask for only the valid packet ID bits, either 255 or maxint

        currentPacketId++;

        currentPacketId = currentPacketId & 0xffffffffL; // keep from exceeding 32 bits

        // Use modulus and +1 to ensure we skip 0 on any values we return
        return (int) ((currentPacketId % numPacketIds) + 1L);
    }

}
