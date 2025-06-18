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
    application
    id("java-conventions")
}

tasks.named<JavaExec>("run") {
    // During the configuration phase of the task we want the argument to be optional because it really is -
    // if we insist on having it present, other "regular" gradle tasks (e.g. "gradlew tasks") are going to
    // fail. However, during the execution phase of the task, the parameter *must* be specified, otherwise
    // we won't know which sample to run. At the same time we do want to provide a user-friendly error message
    // in case the user did not specify -Pexample=x.y.z. For this reason we have to check 2 times whether the property
    // has been specified - the first is for the configuration phase and doFirst{} checks during the execution.

    val exampleArgumentName = "example"
    if (project.hasProperty(exampleArgumentName)) {
        val exampleName = project.findProperty(exampleArgumentName) as String
        val srcDir = project.layout.projectDirectory.dir("src/main/java")

        // first check if the provided example is a file path e.g. src/main/java/com/vmware/vcenter/ListVms.java
        var exampleFile = file(exampleName).absoluteFile
        if (!exampleFile.exists()) {
            // assume the sample is a fully qualified class name - com.vmware.vcenter.ListVms
            exampleFile = srcDir.file(exampleName.replace(".", File.separator) + ".java").asFile
        }

        if (!exampleFile.exists()) {
            throw InvalidUserDataException("Example '$exampleName' could not be found.")
        }

        // transform the absolute path to a fully-qualified class name
        // i.e. PRJ-ROOT/../src/main/java/com/vmware/vcenter/ListVms.java => com.vmware.vcenter.ListVms
        mainClass =
            exampleFile.relativeTo(srcDir.asFile).toString()
                .replace(".java", "")
                .replace(File.separator, ".")
    }

    doFirst {
        if (!project.hasProperty(exampleArgumentName)) {
            throw InvalidUserDataException(
                "Example to run has not been provided. " +
                        "Please re-run the command with -P$exampleArgumentName=... " +
                        "and specify the fully qualified class name or src/main/java/some/package/SomeExample.java",
            )
        }
    }
}
