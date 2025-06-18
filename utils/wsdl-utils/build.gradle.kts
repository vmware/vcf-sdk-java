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
    implementation(libs.angus.mail)
    api(libs.jakarta.ws.api)
    api(libs.cxf.core)
    api(libs.cxf.jaxws)
    api(libs.cxf.http)
    api(libs.cxf.logging)
    api(libs.saaj)
}

val sdkProperties =
    tasks.register<WriteProperties>("writeSdkProperties") {
        destinationFile = layout.buildDirectory.file("generated/sources/vcf_sdk/vcf_sdk_version.properties")
        property("version", version.toString())
    }

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/sdk/common/wsdl/",
            "Implementation-Title" to "com.vmware.sdk.common.wsdl",
        )
    }

    from(sdkProperties)
}
