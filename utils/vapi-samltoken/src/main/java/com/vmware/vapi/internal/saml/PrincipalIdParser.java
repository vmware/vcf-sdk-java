/*
 * ******************************************************************
 * Copyright (c) 2010-2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.vapi.internal.saml;

import java.util.regex.Pattern;

import com.vmware.vapi.internal.saml.exception.ParserException;
import com.vmware.vapi.saml.PrincipalId;

/** Parse principal id from string */
public final class PrincipalIdParser {

    /**
     * Parses a User Principal Name (UPN) string and converts it into a {@link PrincipalId} object. The UPN should be in
     * the format "name@domain".
     *
     * @param upn the User Principal Name string to parse
     * @return a {@link PrincipalId} representing the parsed short name and domain
     * @throws ParserException if the UPN is empty, null, or does not conform to the expected format
     */
    public static PrincipalId parseUpn(String upn) throws ParserException {
        ValidateUtil.validateNotEmpty(upn, "upn");
        int first = upn.indexOf('@');
        int last = upn.lastIndexOf('@');
        int netbios = upn.indexOf('\\');
        if (first == -1 || first == 0 || last == upn.length() - 1 || netbios != -1) {
            throw new ParserException(String.format("Invalid principal value: `%s' (incorrect UPN format)", upn));
        }
        return new PrincipalId(upn.substring(0, last), upn.substring(last + 1));
    }

    /**
     * Parses a group identifier string and converts it into a {@link PrincipalId} object. The group ID can be in the
     * format "domain\\name" or "name/domain".
     *
     * @param groupId the group identifier string to parse
     * @return a {@link PrincipalId} representing the parsed name and domain
     * @throws ParserException if the group ID is empty, null, or does not conform to the expected format
     */
    public static PrincipalId parseGroupId(String groupId) throws ParserException {
        ValidateUtil.validateNotEmpty(groupId, "groupId");

        final PrincipalId group;
        if (groupId.contains("\\")) {
            String[] parts = splitInTwo(groupId, '\\');
            group = new PrincipalId(parts[1], parts[0]);

        } else {
            String[] parts = splitInTwo(groupId, '/');
            group = new PrincipalId(parts[0], parts[1]);
        }

        return group;
    }

    /**
     * Splits the given string into two parts based on the specified separator.
     *
     * @param value the string to split
     * @param separator the character used to split the string
     * @return an array of two strings resulting from the split
     * @throws ParserException if the string does not contain exactly one separator or if any part is empty
     */
    private static String[] splitInTwo(String value, char separator) throws ParserException {

        Pattern splitter = Pattern.compile(Pattern.quote(String.valueOf(separator)));
        String split[] = splitter.split(value, 3);
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ParserException(
                    String.format("Invalid principal value: `%s' (incorrect number of fields)", value));
        }

        return split;
    }
}
