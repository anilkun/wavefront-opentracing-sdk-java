package com.wavefront.opentracing.propagation;

import com.wavefront.opentracing.WavefrontSpanContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.opentracing.propagation.TextMap;

public class B3TextMapPropagator implements Propagator<TextMap> {

    protected static final String TRACE_ID_NAME = "X-B3-TraceId";
    protected static final String SPAN_ID_NAME = "X-B3-SpanId";
    protected static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";
    protected static final String SAMPLED_NAME = "X-B3-Sampled";
    protected static final String FLAGS_NAME = "X-B3-Flags";
    protected static final String BAGGAGE_PREFIX = "baggage-";

    private final String baggagePrefix;

    private B3TextMapPropagator(B3TextMapPropagator.Builder builder) {
        this.baggagePrefix = builder.baggagePrefix;
    }

    @Override
    public WavefrontSpanContext extract(TextMap carrier) {
        UUID traceId = null;
        UUID spanId = null;
        boolean isSampled = false;
        Map<String, String> baggage = null;
        for (Map.Entry<String, String> entry : carrier) {
            if (entry.getKey().equalsIgnoreCase(SAMPLED_NAME)) {
                String value = entry.getValue();
                if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
                    isSampled = true;
                }
            } else if (entry.getKey().equalsIgnoreCase(TRACE_ID_NAME)) {
                traceId = TraceIdUtil.toUuid(entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase(SPAN_ID_NAME)) {
                spanId = TraceIdUtil.toUuid(entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase(FLAGS_NAME)) {
                if (entry.getValue().equals("1")) {
                    isSampled = true;
                }
            } else if (entry.getKey().startsWith(baggagePrefix)) {
                if (baggage == null) {
                    baggage = new HashMap<String, String>();
                }
                baggage.put(entry.getKey().substring(baggagePrefix.length()), entry.getValue());
            }
        }

        if (null != traceId && null != spanId) {
            return new WavefrontSpanContext(traceId, spanId, baggage, isSampled);
        }
        return null;
    }

    @Override
    public void inject(WavefrontSpanContext spanContext, TextMap carrier) {
        carrier.put(TRACE_ID_NAME, TraceIdUtil.uuidToHexStr(spanContext.getTraceId()));
        carrier.put(SPAN_ID_NAME, TraceIdUtil.uuidToHexStr(spanContext.getSpanId()));
        carrier.put(SAMPLED_NAME, spanContext.isSampled() ? "1" : "0");
        for (Map.Entry<String, String> entry : spanContext.baggageItems()) {
            carrier.put(entry.getKey()+baggagePrefix, entry.getValue());
        }
    }

    /**
     * Gets a new {@link B3TextMapPropagator.Builder} instance.
     *
     * @return a {@link B3TextMapPropagator.Builder}
     */
    public static B3TextMapPropagator.Builder builder() {
        return new B3TextMapPropagator.Builder();
    }

    /**
     * A builder for {@link B3TextMapPropagator} instances.
     */
    public static class Builder {
        private String baggagePrefix = BAGGAGE_PREFIX;

        public B3TextMapPropagator.Builder withBaggagePrefix(String baggagePrefix) {
            this.baggagePrefix = baggagePrefix;
            return this;
        }

        /**
         * Builds and returns a B3TextMapPropagator instance based on the given configuration.
         *
         * @return a {@link B3TextMapPropagator}
         */
        public B3TextMapPropagator build() {
            return new B3TextMapPropagator(this);
        }
    }
}
