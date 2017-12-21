package com.vmware.connectors.common.web;

import org.springframework.web.client.HttpClientErrorException;
import rx.Observable;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public final class ObservableUtil {

    private ObservableUtil() {
        // util class
    }

    public static <R> Observable<R> skip404(Throwable throwable) {
        if (throwable instanceof HttpClientErrorException
                && HttpClientErrorException.class.cast(throwable).getStatusCode() == NOT_FOUND) {
            // If it's OK to request non-existent items, proceed; we just won't create a card.
            return Observable.empty();
        } else {
            // If the problem is not 404, let the problem bubble up
            return Observable.error(throwable);
        }
    }

}
