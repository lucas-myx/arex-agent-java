package io.arex.inst.httpclient.webclient.v5;

import io.arex.agent.bootstrap.ctx.TraceTransmitter;
import io.arex.inst.httpclient.common.ArexDataException;
import io.arex.inst.httpclient.common.ExceptionWrapper;
import io.arex.inst.httpclient.common.HttpClientExtractor;
import io.arex.inst.httpclient.common.HttpResponseWrapper;
import io.arex.inst.httpclient.webclient.v5.model.WebClientRequest;
import io.arex.inst.httpclient.webclient.v5.model.WebClientResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebClientWrapper {
    private final ClientRequest httpRequest;
    private final ExchangeStrategies strategies;
    private final Mono<ClientResponse> responseMono;
    private final HttpClientExtractor<ClientRequest, WebClientResponse> extractor;
    private final WebClientAdapter adapter;
    private final TraceTransmitter traceTransmitter;
    private final TraceTransmitter traceTransmitter1;
    private final TraceTransmitter traceTransmitter2;
    public WebClientWrapper(ClientRequest httpRequest, ExchangeStrategies strategies, Mono<ClientResponse> responseMono) {
        this.httpRequest = httpRequest;
        this.strategies = strategies;
        this.adapter = new WebClientAdapter(httpRequest, strategies);
        this.extractor = new HttpClientExtractor<>(this.adapter);
        this.traceTransmitter = TraceTransmitter.create();
        this.traceTransmitter1 = TraceTransmitter.create();
        this.traceTransmitter2 = TraceTransmitter.create();
        this.responseMono = responseMono;
    }

    public Mono<ClientResponse> record() {
        convertRequest();

        return responseMono.doOnCancel(() -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                extractor.record((Exception)null);
            }
        }).doOnError(throwable -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                extractor.record(new ArexDataException(throwable));
            }
        }).map(response -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                List<byte[]> bytes = new ArrayList<>();
                ClientResponse clientResponse = response.mutate().body(body -> {
                    return Flux.defer(() -> Flux.from(body)
                            .map(dataBuffer -> {
                                if (dataBuffer != null) {
                                    bytes.add(dataBuffer.asByteBuffer().array());
                                }
                                return dataBuffer;
                            }).doOnComplete(() -> {
                                byte[] bodyArray = new byte[]{};
                                for (byte[] byteArray : bytes) {
                                    bodyArray = ArrayUtils.addAll(bodyArray, byteArray);
                                }
                                try (TraceTransmitter tm1 = traceTransmitter1.transmit()) {
                                    extractor.record(WebClientResponse.of(response, bodyArray));
                                }
                            })
                    );
                }).build();
                return clientResponse;
            }
        });
    }

    private void convertRequest() {
        WebClientRequest request = WebClientRequest.of(httpRequest);
        Mono<Void> requestMono = httpRequest.writeTo(request, strategies);
        if (requestMono != null) {
            requestMono.subscribe();
            adapter.setHttpRequest(request);
        }
    }

    public Mono replay() {
        try (TraceTransmitter tm2 = traceTransmitter2.transmit()) {
            convertRequest();
            HttpResponseWrapper wrapped = extractor.fetchMockResult();
            if (wrapped == null) {
                return Mono.error(new IOException("not found mock resource"));
            }
            if (wrapped.getException() != null) {
                return Mono.error(unwrap(wrapped.getException()));
            }
            WebClientResponse response = extractor.replay(wrapped);
            return Mono.just(response.originalResponse());
        }
    }

    private IOException unwrap(ExceptionWrapper exception) {
        if (exception.isCancelled()) {
            return new IOException("The request cancelled");
        }
        Exception origin = exception.getOriginalException();
        if (origin instanceof IOException) {
            return (IOException) origin;
        }
        return new IOException(origin);
    }
}