/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.client;

import jakarta.validation.constraints.NotBlank;

public class BaseClientConfig {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String host;

    private String certificatePath;
    private String trustStorePassword;
    private String truststorePath;
    private String truststoreType;
    private String locale = "en-us";
    private String timezone = "PST";
    private String verify = "false";
    private boolean ignoreHostName = true;
    private boolean useInternalApis = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    public String getVerify() {
        return verify;
    }

    public void setVerify(String verify) {
        this.verify = verify;
    }

    public boolean isIgnoreHostName() {
        return ignoreHostName;
    }

    public void setIgnoreHostName(boolean ignoreHostName) {
        this.ignoreHostName = ignoreHostName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isUseInternalApis() {
        return useInternalApis;
    }

    public void setUseInternalApis(boolean useInternalApis) {
        this.useInternalApis = useInternalApis;
    }
}
