/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.ops.config.alertdefinitions;

import com.vmware.sdk.samples.ops.config.symptomdefintions.SymptomDefinitionConfig;

public class AlertDefinitionConfig {

    private String name = "My-Alert-Definition-1";
    private String description = "Alert for CPU demand exceeding 95% on VirtualMachine";
    private String adapterKindKey = "VMWARE";
    private String resourceKindKey = "VirtualMachine";
    private int type = 18;
    private int subType = 19;

    private int waitCycle = 1;
    private int cancelCycle = 1;

    private String severity = "CRITICAL";

    private String impactType = "BADGE";
    private String detail = "health";

    private String relation = "SELF";
    private String aggregation = "ALL";

    private SymptomDefinitionConfig symptomDefinitionConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSubType() {
        return subType;
    }

    public void setSubType(int subType) {
        this.subType = subType;
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

    public String getImpactType() {
        return impactType;
    }

    public void setImpactType(String impactType) {
        this.impactType = impactType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getAggregation() {
        return aggregation;
    }

    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }

    public SymptomDefinitionConfig getSymptomDefinitionConfig() {
        return symptomDefinitionConfig;
    }

    public void setSymptomDefinitionConfig(SymptomDefinitionConfig symptomDefinitionConfig) {
        this.symptomDefinitionConfig = symptomDefinitionConfig;
    }
}
