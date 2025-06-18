/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.sddcm.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringUtil {
    /**
     * Converts Java Object to json String.
     *
     * @param object object to format
     * @return json String made without pretty printer
     */
    public static String toString(Object object) {
        try {
            return new ObjectMapper()
                    .setSerializationInclusion(Include.NON_NULL)
                    .writer()
                    .writeValueAsString(object);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts Java Object to formatted json String using pretty printer.
     *
     * @param object object to format
     * @return formatted json String made with pretty printer
     */
    public static String toPrettyString(Object object) {
        try {
            return new ObjectMapper()
                    .setSerializationInclusion(Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        } catch (Exception e) {
            return null;
        }
    }
}
