package com.wavefront.opentracing.propagation;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

final class TraceIdUtil {

    /**
     * Parses a UUID and converts to BigInteger.
     *
     * @param id UUID of the traceId or spanId
     * @return BigInteger for UUID.
     */
    static BigInteger uuidToBigInteger(UUID id) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return new BigInteger(1, bb.array());
    }


    /**
     * Parses a UUID and converts to BigInteger.
     *
     * @param id UUID of the traceId or spanId
     * @return BigInteger for UUID.
     */
    static String uuidToHexStr(UUID id) {
        BigInteger bi = uuidToBigInteger(id);
        return bi.toString(16);
    }

    /**
     * Parses a full (low + high) traceId, trimming the lower 64 bits.
     * Inspired by {@code io.jaegertracing.internal.propagation}
     *
     * @param hexString a full traceId
     * @return the long value of the higher 64 bits for a 128 bit traceId or 0 for 64 bit traceIds
     */
    private static long high(String hexString) {
        if (hexString.length() <= 16) {
            return 0L;
        }
        int highLength = hexString.length() - 16;
        String highString = hexString.substring(0, highLength);
        return new BigInteger(highString, 16).longValue();
    }

    /**
     * Constructs UUID for traceId/spanId represented as hexString consisting  of (low + high) 64 bits.
     *
     * UUID is generated with long value of  high 64 bits(if any) and long value of low 64 bits and is
     * consistent with the Wavefront approach.
     *
     * @param id hexString form of traceId/spanId
     * @return UUID for traceId/spanId as expected by WavefrontSpanContext
     */
    static UUID toUuid(String id) {
        long idLow = new BigInteger(id, 16).longValue();
        long idHigh = high(id);
        return new UUID(idHigh, idLow);
    }

    TraceIdUtil() {
    }
}
