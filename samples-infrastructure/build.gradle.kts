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
    id("java-conventions")
}

dependencies {
    implementation(libs.slf4j.api)

    implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.0.0.0"))
    implementation("com.vmware.sdk:vmware-sdk-common")

    runtimeOnly(libs.slf4j.jul)
    runtimeOnly(libs.slf4j.jcl)
    runtimeOnly(libs.logback.classic)

    testImplementation(testLibs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
