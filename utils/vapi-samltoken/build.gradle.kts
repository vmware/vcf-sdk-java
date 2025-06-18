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

tasks.named<Javadoc>("javadoc") {
    // don't generate javadoc for oasis & rsa packages - there are too many warnings in the automatically generated code
    source =
        sourceSets.main.get().allSource.filter {
            val packageName = it.relativeTo(project.layout.projectDirectory.file("src/main/java").asFile)
            it.path.endsWith(".java") && packageName.startsWith("com/vmware")
        }.asFileTree
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.cxf.jaxws)
    implementation(libs.angus.mail)
    testImplementation(testLibs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        targetExclude("**/oasis/**", "**/w3/**")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/vapi/saml/",
            "Implementation-Title" to "com.vmware.vapi.saml",
        )
    }
}
