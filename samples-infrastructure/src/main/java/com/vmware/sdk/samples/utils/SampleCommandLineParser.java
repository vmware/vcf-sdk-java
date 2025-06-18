/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleCommandLineParser {
    private static final Logger log = LoggerFactory.getLogger(SampleCommandLineParser.class);

    static {
        configureJulLogger();
    }

    /**
     * Simple helper method that loads command-line parameters into the non-final public static fields of the given
     * class.
     *
     * <p>This method reads command-line arguments and populates public static non-final fields of the given class. The
     * method supports both mandatory and optional parameters. The fields that have default value are expected to be
     * mandatory parameters, the fields that have default value null - optional parameters.
     *
     * <p>The method follows these steps:
     *
     * <ol>
     *   <li>Populates the mandatory parameters and optional parameters.
     *   <li>Generates and prints in slf4j info a help line based on the parameter maps and prints it to the console.
     *   <li>Processes the mandatory parameters and throws an exception if any are missing.
     *   <li>Processes the optional parameters.
     * </ol>
     *
     * @param c The class containing the static fields to be populated with command-line parameters.
     * @param args The command-line arguments to be parsed.
     */
    public static void load(Class<?> c, String[] args) {
        Map<String, Field> mandatoryParameters = new LinkedHashMap<>();
        Map<String, Field> optionalParameters = new LinkedHashMap<>();

        boolean populateInput = populateParameterMaps(c, mandatoryParameters, optionalParameters);
        generateHelpLine(c, mandatoryParameters, optionalParameters);

        Map<String, String> commandLineParameters = getCommandLineParameters(args);
        if (!commandLineParameters.isEmpty()) {
            boolean mandatoryOk = processParameters(mandatoryParameters, commandLineParameters, true);
            boolean optionalOk = processParameters(optionalParameters, commandLineParameters, false);
            if (!populateInput || !mandatoryOk || !optionalOk) {
                throw new RuntimeException("Incorrect input parameters, check the log errors for details. ");
            }
        }
    }

    private static Map<String, String> getCommandLineParameters(String[] args) {
        Map<String, String> result = new HashMap<>();
        if (args.length % 2 == 1) {
            throw new RuntimeException("Wrong number of command line parameters, the number must be even");
        }
        for (int i = 0; i < args.length; i = i + 2) {
            String key = args[i].replace("_", "").replace("-", "").toLowerCase();
            result.put(key, args[i + 1]);
        }
        return result;
    }

    private static boolean populateParameterMaps(
            Class<?> c, Map<String, Field> mandatory, Map<String, Field> optional) {
        boolean ok = true;
        for (Field field : c.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {

                try {
                    Object defaultValue = field.get(null);
                    String fieldName = field.getName().toLowerCase();

                    if (defaultValue != null) {
                        mandatory.put(fieldName, field);
                    } else {
                        optional.put(fieldName, field);
                    }
                } catch (IllegalAccessException e) {
                    log.error("Unable to access field: {}", field.getName());
                    ok = false;
                }
            }
        }
        return ok;
    }

    private static boolean processParameters(
            Map<String, Field> parameters, Map<String, String> args, boolean isMandatory) {
        boolean ok = true;
        for (Map.Entry<String, Field> entry : parameters.entrySet()) {
            Field field = entry.getValue();
            String value = args.get(entry.getKey().replace("_", "").toLowerCase());

            if (isMandatory && value == null) {
                log.error("Missing mandatory parameter: {} ", field.getName());
                ok = false;
            }

            if (value != null) {
                try {
                    Object typedValue = convertToFieldType(value, field.getType());
                    field.set(null, typedValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.error("Cannot set a parameter: {}", field.getName());
                    ok = false;
                }
            }
        }
        return ok;
    }

    private static void generateHelpLine(Class<?> c, Map<String, Field> mandatory, Map<String, Field> optional) {
        StringBuilder helpLine = new StringBuilder();
        helpLine.append(c.getSimpleName()).append(" ");

        for (Field field : mandatory.values()) {
            appendHelpParameter(helpLine, field, "--", " ");
        }

        for (Field field : optional.values()) {
            appendHelpParameter(helpLine, field, "[--", "] ");
        }
        log.info("Syntax: {}", helpLine);
    }

    private static void appendHelpParameter(StringBuilder helpLine, Field field, String prefix, String suffix) {
        helpLine.append(prefix).append(field.getName()).append(" ");

        if (field.getType().isArray()) {
            helpLine.append("v1,v2");
        } else {
            helpLine.append("v");
        }
        helpLine.append(suffix);
    }

    private static Object convertToFieldType(String value, Class<?> fieldType) {
        if (fieldType == Integer.class || fieldType == Integer.TYPE) {
            return Integer.parseInt(value);
        } else if (fieldType == Long.class || fieldType == Long.TYPE) {
            return Long.parseLong(value);
        } else if (fieldType == Double.class || fieldType == Double.TYPE) {
            return Double.parseDouble(value);
        } else if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
            return Boolean.parseBoolean(value);
        } else if (fieldType == String.class) {
            return value;
        } else if (fieldType == String[].class) {
            return value.split(",");
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * jaxws-rt uses java.util.logging to log messages. To make it use slf4j + logback, we need to re-configure the
     * LogManager to add bridge handler (coming from jul-to-slf4j). This makes it possible to use logback.xml to
     * configure jaxws logging.
     */
    public static void configureJulLogger() {
        try (InputStream is =
                SampleCommandLineParser.class.getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            System.err.println("Could not configure java.util.logging to use slf4j/logback");
            e.printStackTrace(System.err);
        }
    }
}
