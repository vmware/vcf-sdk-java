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

// Use the group name from vsphere-automation-sdk-java for continuity
group = "com.vmware.vapi"
version = sdkLibs.versions.vapi.get()

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.cxf.jaxws)
    implementation(libs.angus.mail)
    implementation(sdkLibs.vapi.runtime)
    implementation(project(":utils:vapi-samltoken"))
    testImplementation(testLibs.junit)
    testImplementation(testLibs.easymock)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/vapi/cis/",
            "Implementation-Title" to "com.vmware.vapi.cis",
        )
    }
}
