/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

/*
 * Build configurations for improving IDE integration.
 */
plugins {
    eclipse
}

/*
 * Enables Eclipse project files generation via
 *
 *     gradlew cleanEclipse eclipse [-Peclipse.classpath.vars]
 *
 * If -Peclipse.classpath.vars is used, VCF_SDK_GRADLE_USER_HOME must be declared as Eclipse
 * Classpath Variable pointing to the Gradle User Home.
 */

apply(plugin = "eclipse")

eclipse {
    if (hasProperty("eclipse.classpath.vars")) {
        pathVariables(mapOf("VCF_SDK_GRADLE_USER_HOME" to file(gradle.gradleUserHomeDir)))
    }
}
// Improvements for IntelliJ, VS Code, etc should go here (if needed)
