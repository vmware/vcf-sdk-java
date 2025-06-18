/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

plugins {
    `java-library`
    id("util-conventions")
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(sdkLibs.vapi.runtime)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/sdk/common/",
            "Implementation-Title" to "com.vmware.sdk.common",
        )
    }
}
