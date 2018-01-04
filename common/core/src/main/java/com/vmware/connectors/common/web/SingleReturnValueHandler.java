/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import rx.Single;
import rx.functions.Func1;

/**
 * Created by Rob Worsnop on 10/31/17.
 */
public class SingleReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

    @Override
    public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
        return returnValue != null && supportsReturnType(returnType);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return Single.class.isAssignableFrom(returnType.getParameterType())
                || isResponseEntity(returnType);
    }

    private boolean isResponseEntity(MethodParameter returnType) {
        if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            Class<?> bodyType = ResolvableType.forMethodParameter(returnType)
                    .getGeneric(0).resolve();
            return bodyType != null && Single.class.isAssignableFrom(bodyType);
        }
        return false;
    }

    @Override
    @SuppressWarnings("PMD") // Not good practice, but this class goes away when we move to Spring 5/Reactor
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                  ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
            throws Exception {

        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }

        ResponseEntity<Single<?>> responseEntity = getResponseEntity(returnValue);
        if (responseEntity != null) {
            returnValue = responseEntity.getBody();
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                return;
            }
        }

        final Single<?> single = Single.class.cast(returnValue);
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(
                convertToDeferredResult(responseEntity, single), mavContainer);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Single<?>> getResponseEntity(Object returnValue) {
        if (ResponseEntity.class.isAssignableFrom(returnValue.getClass())) {
            return (ResponseEntity<Single<?>>) returnValue;

        }
        return null;
    }

    protected DeferredResult<?> convertToDeferredResult(
            final ResponseEntity<Single<?>> responseEntity, Single<?> single) {

        Single<ResponseEntity<?>> singleResponse = single
                .map(new Func1<Object, ResponseEntity<?>>() {
                    @Override
                    public ResponseEntity<?> call(Object object) {
                        if (object instanceof ResponseEntity) {
                            return (ResponseEntity) object;
                        }

                        return new ResponseEntity<Object>(object,
                                getHttpHeaders(responseEntity),
                                getHttpStatus(responseEntity));
                    }
                });

        return new SingleDeferredResult<>(singleResponse);
    }

    private HttpStatus getHttpStatus(ResponseEntity<?> responseEntity) {
        if (responseEntity == null) {
            return HttpStatus.OK;
        }
        return responseEntity.getStatusCode();
    }

    private HttpHeaders getHttpHeaders(ResponseEntity<?> responseEntity) {
        if (responseEntity == null) {
            return new HttpHeaders();
        }
        return responseEntity.getHeaders();
    }
}
