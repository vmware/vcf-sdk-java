plugins {
    `java-platform`
    `maven-publish`
}

tasks.wrapper {
    gradleVersion = "8.7"
}

dependencies {
    constraints {
        val libsCatalog = versionCatalogs.named("sdkLibs")

        libsCatalog.libraryAliases.forEach {
            val library = libsCatalog.findLibrary(it).get().get()
            api(library)
        }

        project.gradle.projectsEvaluated {
            subprojects.forEach {
                if (it.project.parent?.name == "utils") {
                    // If the project is in under utils, include it in the BOM.
                    api("${it.project.group}:${it.project.name}:${it.project.version}")
                }
            }
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("sdk") {
            from(components["javaPlatform"])
            pom {
                group = "com.vmware.sdk"
                artifactId = "vcf-sdk-bom"
                version = versionCatalogs.named("sdkLibs").findVersion("sdk").get().toString()
                name = "VCF SDK - BOM"
                description = "Bill of materials to make sure a consistent set of versions is used for VCF SDK  modules."
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
        }
    }
}
