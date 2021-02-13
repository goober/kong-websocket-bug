package com.resurs.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
public class LoggingTraceRepository implements HttpTraceRepository {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingTraceRepository.class);
    private final HttpTraceRepository delegate = new InMemoryHttpTraceRepository();

    @Override
    public List<HttpTrace> findAll() {
        return delegate.findAll();
    }

    @Override
    public void add(HttpTrace traceInfo) {
        LOG.info(traceInfo.toString());
        this.delegate.add(traceInfo);
    }
}
