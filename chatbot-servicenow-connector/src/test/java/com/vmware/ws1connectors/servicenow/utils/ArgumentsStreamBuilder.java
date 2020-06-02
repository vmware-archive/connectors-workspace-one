/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ArgumentsStreamBuilder implements Stream.Builder<Arguments> {

    private List<Arguments> argsList;

    public ArgumentsStreamBuilder() {
        argsList = new ArrayList<>();
    }

    @Override public void accept(Arguments arguments) {
        argsList.add(arguments);
    }

    public ArgumentsStreamBuilder add(Object... arguments) {
        checkNotNull(arguments, "Arguments cannot be null");
        accept(Arguments.of(arguments));
        return this;
    }

    public ArgumentsStreamBuilder addAll(Stream<Arguments> argumentsStream) {
        checkNotNull(argumentsStream, "Arguments stream can't be null.");
        argumentsStream.forEach(this);
        return this;
    }

    @Override public Stream<Arguments> build() {
        return argsList.stream();
    }
}
