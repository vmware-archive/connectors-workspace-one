/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * Created by Rob Worsnop on 7/27/17.
 */
@RestController
public class RootController {

    @GetMapping(path = "/", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<ResourceSupport> getRoot(HttpServletRequest servletRequest) {
        HttpRequest request = new ServletServerHttpRequest(servletRequest);
        ResourceSupport resource = new ResourceSupport();
        String metadata = UriComponentsBuilder.fromHttpRequest(request).path("/discovery/metadata.hal").build().toUriString();
        resource.add(new Link(metadata, "metadata"));
        String cards = UriComponentsBuilder.fromHttpRequest(request).path("/cards/requests").build().toUriString();
        resource.add(new Link(cards, "cards"));
        String image = UriComponentsBuilder.fromHttpRequest(request).path("/images/connector.png").build().toUriString();
        resource.add(new Link(image, "image"));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, HOURS))
                .body(resource);
    }
}
