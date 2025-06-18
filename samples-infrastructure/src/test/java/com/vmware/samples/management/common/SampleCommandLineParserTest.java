/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.samples.management.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;

@Isolated
public class SampleCommandLineParserTest {

    public static class FancySDKSample {
        public static int intPrimitiveField;
        public static Integer intField;
        public static long longPrimitiveField;
        public static Long longField;
        public static double doublePrimitiveField;
        public static Double doubleField;
        public static boolean booleanPrimitiveField;
        public static Boolean booleanField;
        public static String stringField;
        public static String[] stringArrayField;
    }

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent, false, StandardCharsets.UTF_8));
    }

    @BeforeEach
    public void tearDown() {
        System.setOut(originalOut);
        System.out.println(outContent);
    }

    private void resetFieldsNonPrimitivesNull() {
        FancySDKSample.intPrimitiveField = 0;
        FancySDKSample.intField = null;
        FancySDKSample.longPrimitiveField = 0L;
        FancySDKSample.longField = null;
        FancySDKSample.doublePrimitiveField = 0.0;
        FancySDKSample.doubleField = null;
        FancySDKSample.booleanPrimitiveField = false;
        FancySDKSample.booleanField = null;
        FancySDKSample.stringField = null;
        FancySDKSample.stringArrayField = null;
    }

    private void resetFieldsNonPrimitivesSet() {
        FancySDKSample.intPrimitiveField = 0;
        FancySDKSample.longPrimitiveField = 0L;
        FancySDKSample.doublePrimitiveField = 0.0;
        FancySDKSample.booleanPrimitiveField = false;
        FancySDKSample.intField = 1;
        FancySDKSample.longField = 1L;
        FancySDKSample.doubleField = 1D;
        FancySDKSample.booleanField = true;
        FancySDKSample.stringField = "";
    }

    @Test
    public void testLoadWithValidArguments() {
        resetFieldsNonPrimitivesSet();
        String[] args = {
            "--intprimitivefield", "42",
            "--intfield", "43",
            "--longprimitivefield", "100000",
            "--longfield", "100001",
            "--doubleprimitivefield", "3.14",
            "--doublefield", "6.28",
            "--booleanprimitivefield", "true",
            "--booleanfield", "false",
            "--stringfield", "hello_World",
            "--stringarrayfield", "value1,value2"
        };

        SampleCommandLineParser.load(FancySDKSample.class, args);
        assertEquals(42, FancySDKSample.intPrimitiveField);
        assertEquals(43, FancySDKSample.intField);
        assertEquals(100000L, FancySDKSample.longPrimitiveField);
        assertEquals(100001L, FancySDKSample.longField);
        assertEquals(3.14, FancySDKSample.doublePrimitiveField);
        assertEquals(6.28, FancySDKSample.doubleField);
        assertTrue(FancySDKSample.booleanPrimitiveField);
        assertFalse(FancySDKSample.booleanField);
        assertEquals("hello_World", FancySDKSample.stringField);
        assertArrayEquals(new String[] {"value1", "value2"}, FancySDKSample.stringArrayField);
    }

    @Test
    public void testLoadWithMissingPrimitive() {
        resetFieldsNonPrimitivesNull();
        String[] argsOnlySomePrimitives = {
            "--intprimitivefield", "42",
            "--longprimitivefield", "100000",
        };
        assertThrows(
                RuntimeException.class,
                () -> SampleCommandLineParser.load(FancySDKSample.class, argsOnlySomePrimitives));
        assertTrue(outContent
                .toString(StandardCharsets.UTF_8)
                .contains("Missing mandatory parameter: doublePrimitiveField"));
        assertTrue(outContent
                .toString(StandardCharsets.UTF_8)
                .contains("Missing mandatory parameter: booleanPrimitiveField"));
    }

    @Test
    public void testLoadWithMissingMandatoryArgumentsNonPrimitive() {
        resetFieldsNonPrimitivesNull();
        String[] argsAllPrimitives = {
            "--intprimitivefield", "42",
            "--longprimitivefield", "100000",
            "--doubleprimitivefield", "3.14",
            "--booleanprimitivefield", "true",
        };
        FancySDKSample.intField = 1;
        assertThrows(
                RuntimeException.class, () -> SampleCommandLineParser.load(FancySDKSample.class, argsAllPrimitives));
        assertTrue(outContent.toString(StandardCharsets.UTF_8).contains("Missing mandatory parameter: intField "));
    }

    @Test
    public void testMissingOptionalParameter() {
        resetFieldsNonPrimitivesNull();
        String[] argsAllPrimitives = {
            "--intprimitivefield", "42",
            "--longprimitivefield", "100000",
            "--doubleprimitivefield", "3.14",
            "--booleanprimitivefield", "true",
        };
        SampleCommandLineParser.load(FancySDKSample.class, argsAllPrimitives);
    }

    @Test
    public void testMultipleParameterFormat() {
        resetFieldsNonPrimitivesNull();
        String[] argsAllPrimitives = {
            "--int_primitive_field", "42",
            "--longPRIMITIVEfield", "100000",
            "--doublePrimitiveField", "3.14",
            "--BOOLEANPRIMITIVEFIELD", "true",
        };
        SampleCommandLineParser.load(FancySDKSample.class, argsAllPrimitives);
    }

    @Test
    public void testHelpLineOutput() {
        resetFieldsNonPrimitivesNull();
        // Initialize mandatory fields (primitive types)
        String[] argsAllPrimitives = {
            "--intprimitivefield", "42",
            "--longprimitivefield", "100000",
            "--doubleprimitivefield", "3.14",
            "--booleanprimitivefield", "true",
        };
        SampleCommandLineParser.load(FancySDKSample.class, argsAllPrimitives);
        String expectedHelpLine =
                "FancySDKSample --intPrimitiveField v --longPrimitiveField v --doublePrimitiveField v --booleanPrimitiveField v [--intField v] [--longField v] [--doubleField v] [--booleanField v] [--stringField v] [--stringArrayField v1,v2]";
        String actualHelpLine = outContent.toString(StandardCharsets.UTF_8);

        assertTrue(actualHelpLine.contains(expectedHelpLine));
    }

    @Test
    public void testLoadWithInvalidArguments() {
        String[] args = {
            "--intprimitivefield", "notAnInt", // wrong integer
            "--booleanprimitivefield", "notABoolean" // wrong boolean
        };

        assertThrows(RuntimeException.class, () -> SampleCommandLineParser.load(FancySDKSample.class, args));
        assertTrue(outContent.toString(StandardCharsets.UTF_8).contains("Cannot set a parameter: intPrimitiveField"));
    }
}
