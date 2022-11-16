package io.arex.inst.httpclient.webclient.v5.util;

import org.reactivestreams.Publisher;
import org.springframework.core.codec.CodecException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

public class WebClientUtils {
    private static final String VALUE_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

    /**
     * Predicate that returns true if an exception should be wrapped.
     */
    public final static Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
            t -> !(t instanceof WebClientException) && !(t instanceof CodecException);


    /**
     * Map the given response to a single value {@code ResponseEntity<T>}.
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<ResponseEntity<T>> mapToEntity(ClientResponse response, Mono<T> bodyMono) {
        return ((Mono<Object>) bodyMono).defaultIfEmpty(VALUE_NONE).map(body ->
                new ResponseEntity<>(
                        body != VALUE_NONE ? (T) body : null,
                        response.headers().asHttpHeaders(),
                        response.rawStatusCode()));
    }

    /**
     * Map the given response to a {@code ResponseEntity<List<T>>}.
     */
    public static <T> Mono<ResponseEntity<List<T>>> mapToEntityList(ClientResponse response, Publisher<T> body) {
        return Flux.from(body).collectList().map(list ->
                new ResponseEntity<>(list, response.headers().asHttpHeaders(), response.rawStatusCode()));
    }
}
