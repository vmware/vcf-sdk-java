/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.symptomdefintions;

public class SymptomDefinitionConfig {

    private String name = "Symptom-Definition-1";

    private String adapterKindKey = "VMWARE";
    private String resourceKindKey = "VirtualMachine";

    private int waitCycle = 5;
    private int cancelCycle = 5;

    private String severity = "CRITICAL";

    private String key = "cpu|demandmhz";
    private String operator = "GT_EQ";
    private String value = "95";
    private boolean instance = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdapterKindKey() {
        return adapterKindKey;
    }

    public void setAdapterKindKey(String adapterKindKey) {
        this.adapterKindKey = adapterKindKey;
    }

    public String getResourceKindKey() {
        return resourceKindKey;
    }

    public void setResourceKindKey(String resourceKindKey) {
        this.resourceKindKey = resourceKindKey;
    }

    public int getWaitCycle() {
        return waitCycle;
    }

    public void setWaitCycle(int waitCycle) {
        this.waitCycle = waitCycle;
    }

    public int getCancelCycle() {
        return cancelCycle;
    }

    public void setCancelCycle(int cancelCycle) {
        this.cancelCycle = cancelCycle;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isInstance() {
        return instance;
    }

    public void setInstance(boolean instance) {
        this.instance = instance;
    }
}
