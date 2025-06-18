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
    id("application-conventions")
}

dependencies {
    // The BOM provides vsphere-utils which will transitively pull in bindings and other code.
    implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.0.0.0"))
    implementation("com.vmware.sdk:vsphere-utils")
    implementation("com.vmware.sdk:vsan-data-protection")

    // Needed for argument parsing, logback/jul config, etc.
    implementation(project(":samples-infrastructure"))
}
