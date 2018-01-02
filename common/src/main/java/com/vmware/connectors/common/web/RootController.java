/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

    private final boolean hasTestAuth;

    @Autowired
    public RootController(@Value("${connector.hasTestAuth:false}") boolean hasTestAuth) {
        this.hasTestAuth = hasTestAuth;
    }

    @GetMapping(path = "/", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<ResourceSupport> getRoot(HttpServletRequest servletRequest) {
        HttpRequest request = new ServletServerHttpRequest(servletRequest);
        ResourceSupport resource = new ResourceSupport();

        addMetadata(resource, request);
        addCards(resource, request);
        addImage(resource, request);
        addAuth(resource, request);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, HOURS))
                .body(resource);
    }

    private void addMetadata(ResourceSupport resource, HttpRequest request) {
        String metadata = UriComponentsBuilder.fromHttpRequest(request).path("/discovery/metadata.hal").build().toUriString();
        resource.add(new Link(metadata, "metadata"));
    }

    private void addCards(ResourceSupport resource, HttpRequest request) {
        String cards = UriComponentsBuilder.fromHttpRequest(request).path("/cards/requests").build().toUriString();
        resource.add(new Link(cards, "cards"));
    }

    private void addImage(ResourceSupport resource, HttpRequest request) {
        Resource imageResource = new ClassPathResource("/static/images/connector.png");
        if (imageResource.exists()) {
            String image = UriComponentsBuilder.fromHttpRequest(request).path("/images/connector.png").build().toUriString();
            resource.add(new Link(image, "image"));
        }
    }

    private void addAuth(ResourceSupport resource, HttpRequest request) {
        if (hasTestAuth) {
            String testAuth = UriComponentsBuilder.fromHttpRequest(request).path("/test-auth").build().toUriString();
            resource.add(new Link(testAuth, "test_auth"));
        }
    }

}
