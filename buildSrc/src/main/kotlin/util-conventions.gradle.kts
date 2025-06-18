import java.net.InetAddress

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
    id("maven-publish")
}

group = "com.vmware.sdk"
version = versionCatalogs.named("sdkLibs").findVersion("sdk").get()

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                // nice to have fields
                name = project.name
                description = "${project.name.replace("-utils", "")} utility code"
                url = "https://vmware.com"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        organization = "Broadcom Inc."
                        id = "vmware.sdk@broadcom.com"
                        name = "VCF Java SDK Team"
                        email = "vmware.sdk@broadcom.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/vmware/vcf-sdk-java.git"
                    developerConnection = "scm:git:ssh://github.com/vmware/vcf-sdk-java.git"
                    url = "https://github.com/vmware/vcf-sdk-java"
                }
            }

            from(components["java"])
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.named<Jar>("jar") {
    var buildNumber = System.getenv("BUILD_NUMBER")
    if (buildNumber == null) {
        buildNumber = "${InetAddress.getLocalHost().hostName}-${System.currentTimeMillis()}"
    }
    manifest {
        attributes(
            "Specification-Title" to "${project.name.replace("-utils", "")} utility code",
            "Specification-Version" to version,
            "Specification-Vendor" to "Broadcom Inc.",
            "Implementation-Version" to buildNumber,
            "Implementation-Vendor" to "Broadcom Inc.",
        )
    }
    from(project.rootDir) {
        include("LICENSE", "NOTICE")
        into("META-INF")
    }
}

tasks.named<Jar>("sourcesJar") {
    from(project.rootDir) {
        include("LICENSE", "NOTICE")
        into("META-INF")
    }
}
