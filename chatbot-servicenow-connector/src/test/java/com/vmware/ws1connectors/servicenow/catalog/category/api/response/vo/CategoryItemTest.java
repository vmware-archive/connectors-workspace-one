/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.vmware.ws1connectors.servicenow.utils.JsonUtils.convertFromJsonFile;

class CategoryItemTest {

    @Test void getsDescriptionInPlainTextForDescriptionInHtml() {
        final String plainTextDesc = "Asus G Series Notebook\nIntel Core i7-2630QM 2.0GHz\n"
            + "8GB Memory 500GB HDD\nNVIDIA GeForce GTX 460M\nDVD Super Multi";
        final CategoryItem categoryItem = convertFromJsonFile("botflows/servicenow/response/laptop_item.json", CategoryItem.class);
        Assertions.assertThat(categoryItem.getDescription()).isEqualTo(plainTextDesc);
    }

}
