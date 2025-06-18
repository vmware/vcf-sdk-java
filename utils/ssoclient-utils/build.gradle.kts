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
    api(sdkLibs.ssoclient)
    implementation(libs.slf4j.api)
    implementation(project(":utils:wsdl-utils"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/sdk/ssoclient/",
            "Implementation-Title" to "com.vmware.sdk.ssoclient",
        )
    }
}
