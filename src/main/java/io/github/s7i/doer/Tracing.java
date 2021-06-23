package io.github.s7i.doer;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;

public enum Tracing {

    INSTANCE;

    private Tracer tracer;

    Tracing() {
        tracer = new Configuration("doer")
              .getTracer();
    }

    public Tracer getTracer() {
        return tracer;
    }
}
