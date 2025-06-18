import com.diffplug.spotless.LineEnding

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
    id("java")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
    id("ide-conventions")
}

repositories {
    mavenLocal()
    maven {
        // The presence of this "maven" folder (in the root of the project) is optional and depends on whether the
        // code was distributed via Broadcom's developer portal (vcf-sdk-java.zip) or Git.
        //
        // The vcf-sdk-java.zip archive includes a copy of the bindings and the utility code. This makes it possible
        // for developers to access the vmware-provided artifacts in "air gapped" environments. To resolve the additional
        // dependencies (e.g. Apache's httpclient), new repository should be defined, for example the company's internal one.
        //
        // The Git repository does not have a "maven" dir because it contains pre-compiled JARs. In this case all
        // dependencies can be fetched from Maven Central.
        url = File(project.rootDir, "maven").toURI()
    }
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all", true)
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:-missing", true)

}

spotless {
    val license = """/*
 * ******************************************************************
 * Copyright (c) ${'$'}YEAR Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

"""

    java {
        palantirJavaFormat("2.47.0").formatJavadoc(true)
        // Eclipse-like format rules:
        // static all other,
        // blank,
        // java.*,
        // blank,
        // javax.*,
        // blank,
        // jakarta.*,
        // blank,
        // org.*,
        // blank,
        // com.*,
        // blank,
        // all other imports
        importOrder("#", "java", "javax", "jakarta", "org", "com", "")
            .semanticSort(true) // group imports by package, instead of lexicographically

        removeUnusedImports()

        lineEndings = LineEnding.UNIX

        licenseHeader(license).updateYearWithLatest(false)

        custom("NoWildcardImports") { input ->
            val lines = input.lines()
            var wildcardImportFound = false

            lines.forEachIndexed { index, line ->
                if (line.startsWith("import ") && line.endsWith(".*;") && !line.trim().startsWith("//")) {
                    val lineNumber = index + 1
                    println("  --> Wildcard import found at line $lineNumber: $line")
                    wildcardImportFound = true
                }
            }

            if (wildcardImportFound) {
                println()
                println("Please manually remove all wildcard imports.")
                val err = StopExecutionException("Build failed due to wildcard imports.")
                err.stackTrace = arrayOf()
                throw err
            }

            input
        }
    }

    spotbugs {
        effort.set(com.github.spotbugs.snom.Effort.LESS)
        reportLevel = com.github.spotbugs.snom.Confidence.valueOf("MEDIUM")
        excludeFilter = file("${rootDir}/buildSrc/src/main/resources/samples.spotbugs-exclude.xml")
        reportsDir = file("${layout.buildDirectory}/spotbugs")
        onlyAnalyze = listOf("com.vmware.-")
    }

    dependencies {
        spotbugsPlugins("jp.skypencil.findbugs.slf4j:bug-pattern:1.4.2@jar")
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint()

        lineEndings = LineEnding.UNIX

        licenseHeader(license, "(import|plugins)").updateYearWithLatest(false)
    }
}
