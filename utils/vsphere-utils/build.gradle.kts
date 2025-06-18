/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarFile
import kotlin.system.measureTimeMillis

plugins {
    `java-library`
    id("util-conventions")
}

dependencies {
    api(sdkLibs.vim25)
    api(sdkLibs.vcenter)
    api(sdkLibs.pbm)
    api(sdkLibs.sms)
    api(sdkLibs.vslm)
    api(project(":utils:wsdl-utils"))
    api(project(":utils:ssoclient-utils"))
    api(project(":utils:vapi-authentication"))
    api(project(":utils:vapi-samltoken"))
    api(project(":utils:vmware-sdk-common"))
}

spotless {
    java {
        targetExclude("**/generated/**/*.java")
    }
}

buildscript {
    dependencies {
        classpath("org.ow2.asm:asm:9.7.1")
    }
}

val generateVim25ClassesTask =
    tasks.register("generateVim25Class") {
        group = "build"
        description = "Generates a Java class listing all com.vmware.vim25 classes."
        val generatedPackage = "com.vmware.sdk.vsphere.client.bindings"
        val generatedDir = generatedPackage.replace('.', '/')
        val generatedClassName = "Vim25Classes"
        val generatedSourcesRoot = "generated/sources/java"
        val generatedSourcesRootDir = layout.buildDirectory.dir(generatedSourcesRoot).get().asFile
        val generatedSourcesDir = layout.buildDirectory.dir("$generatedSourcesRoot/$generatedDir").get().asFile
        val libsCatalog = versionCatalogs.named("sdkLibs")
        val vim25Dependency = libsCatalog.findLibrary("vim25").get().get()
        val vim25Jar =
            sourceSets["main"].runtimeClasspath.files.find {
                it.name == "${vim25Dependency.name}-${vim25Dependency.version}.jar"
            }!!
        inputs.file(vim25Jar)
        outputs.dir(generatedSourcesRootDir)
        doLast {
            fun hasXmlTypeAnnotation(classBytes: ByteArray): Boolean {
                val classReader = ClassReader(classBytes)
                var hasAnnotation = false
                classReader.accept(
                    object : ClassVisitor(Opcodes.ASM9) {
                        override fun visitAnnotation(
                            descriptor: String?,
                            visible: Boolean,
                        ): AnnotationVisitor? {
                            if (descriptor == "Ljakarta/xml/bind/annotation/XmlType;") {
                                hasAnnotation = true
                            }
                            return super.visitAnnotation(descriptor, visible)
                        }
                    },
                    0,
                )
                return hasAnnotation
            }

            val executionTime =
                measureTimeMillis {
                    val classNames = mutableSetOf<String>()

                    logger.info("Processing JAR: ${vim25Jar.absolutePath}")
                    JarFile(vim25Jar).use { jar ->
                        jar.entries().asSequence()
                            .filter { it.name.endsWith(".class") && !it.isDirectory }
                            .forEach {
                                val className = it.name.replace('/', '.').removeSuffix(".class")
                                if (className.startsWith("com.vmware.vim25.")) {
                                    val classBytes = jar.getInputStream(it).readBytes()
                                    if (hasXmlTypeAnnotation(classBytes)) {
                                        classNames.add(className)
                                    }
                                }
                            }
                    }

                    val sortedClassNames = classNames.sorted()
                    val classContent =
                        buildString {
                            appendLine("// Automatically generated file")
                            appendLine("package $generatedPackage;")
                            appendLine()
                            appendLine("public class $generatedClassName {")
                            appendLine("    public static Class<?>[] getClasses() {")
                            appendLine("        return new Class<?>[] {")
                            sortedClassNames.forEach { className ->
                                appendLine("            $className.class,")
                            }
                            appendLine("        };")
                            appendLine("    }")
                            appendLine("}")
                        }
                    generatedSourcesDir.mkdirs()
                    val outputFile = File(generatedSourcesDir, "$generatedClassName.java")
                    outputFile.writeText(classContent)
                    logger.lifecycle("Generated file: ${outputFile.absolutePath}")
                }
            logger.lifecycle("Task 'generateVim25Class' completed in ${executionTime}ms.")
        }
    }

sourceSets {
    main {
        java {
            srcDir(generateVim25ClassesTask)
        }
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Name" to "com/vmware/sdk/vsphere/",
            "Implementation-Title" to "com.vmware.sdk.vsphere",
        )
    }
}
