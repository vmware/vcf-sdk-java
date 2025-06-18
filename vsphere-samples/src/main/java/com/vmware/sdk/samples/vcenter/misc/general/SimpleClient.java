/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.general;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.PropertyCollectorHelper;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

/** This sample lists the inventory contents (managed entities). */
public class SimpleClient {
    private static final Logger log = LoggerFactory.getLogger(SimpleClient.class);
    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";
    /** REQUIRED: Username to log in to the vCenter Server. */
    public static String username = "username";
    /** REQUIRED: Password to log in to the vCenter Server. */
    public static String password = "password";
    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (do not leave it empty on production environments).
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(SimpleClient.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            ServiceContent serviceContent = client.getVimServiceContent();
            PropertyCollectorHelper propertyCollectorHelper = new PropertyCollectorHelper(vimPort, serviceContent);

            getAndPrintInventoryContents(serviceContent, propertyCollectorHelper);
        }
    }

    private static void getAndPrintInventoryContents(
            ServiceContent serviceContent, PropertyCollectorHelper propertyCollectorHelper)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        TraversalSpec resourcePoolTraversalSpec = new TraversalSpec();
        resourcePoolTraversalSpec.setName("resourcePoolTraversalSpec");
        resourcePoolTraversalSpec.setType("ResourcePool");
        resourcePoolTraversalSpec.setPath("resourcePool");
        resourcePoolTraversalSpec.setSkip(Boolean.FALSE);

        SelectionSpec rpts = new SelectionSpec();
        rpts.setName("resourcePoolTraversalSpec");
        resourcePoolTraversalSpec.getSelectSet().add(rpts);

        TraversalSpec computeResourceRpTraversalSpec = new TraversalSpec();
        computeResourceRpTraversalSpec.setName("computeResourceRpTraversalSpec");
        computeResourceRpTraversalSpec.setType("ComputeResource");
        computeResourceRpTraversalSpec.setPath("resourcePool");
        computeResourceRpTraversalSpec.setSkip(Boolean.FALSE);

        SelectionSpec rptss = new SelectionSpec();
        rptss.setName("resourcePoolTraversalSpec");
        computeResourceRpTraversalSpec.getSelectSet().add(rptss);

        TraversalSpec computeResourceHostTraversalSpec = new TraversalSpec();
        computeResourceHostTraversalSpec.setName("computeResourceHostTraversalSpec");
        computeResourceHostTraversalSpec.setType("ComputeResource");
        computeResourceHostTraversalSpec.setPath("host");
        computeResourceHostTraversalSpec.setSkip(Boolean.FALSE);

        TraversalSpec datacenterHostTraversalSpec = new TraversalSpec();
        datacenterHostTraversalSpec.setName("datacenterHostTraversalSpec");
        datacenterHostTraversalSpec.setType("Datacenter");
        datacenterHostTraversalSpec.setPath("hostFolder");
        datacenterHostTraversalSpec.setSkip(Boolean.FALSE);

        SelectionSpec ftspec = new SelectionSpec();
        ftspec.setName("folderTraversalSpec");
        datacenterHostTraversalSpec.getSelectSet().add(ftspec);

        TraversalSpec datacenterVmTraversalSpec = new TraversalSpec();
        datacenterVmTraversalSpec.setName("datacenterVmTraversalSpec");
        datacenterVmTraversalSpec.setType("Datacenter");
        datacenterVmTraversalSpec.setPath("vmFolder");
        datacenterVmTraversalSpec.setSkip(Boolean.FALSE);

        SelectionSpec ftspecs = new SelectionSpec();
        ftspecs.setName("folderTraversalSpec");
        datacenterVmTraversalSpec.getSelectSet().add(ftspecs);

        TraversalSpec folderTraversalSpec = new TraversalSpec();
        folderTraversalSpec.setName("folderTraversalSpec");
        folderTraversalSpec.setType("Folder");
        folderTraversalSpec.setPath("childEntity");
        folderTraversalSpec.setSkip(Boolean.FALSE);

        SelectionSpec ftrspec = new SelectionSpec();
        ftrspec.setName("folderTraversalSpec");

        List<SelectionSpec> ssarray = new ArrayList<>();
        ssarray.add(ftrspec);
        ssarray.add(datacenterHostTraversalSpec);
        ssarray.add(datacenterVmTraversalSpec);
        ssarray.add(computeResourceRpTraversalSpec);
        ssarray.add(computeResourceHostTraversalSpec);
        ssarray.add(resourcePoolTraversalSpec);

        folderTraversalSpec.getSelectSet().addAll(ssarray);

        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(Boolean.FALSE);
        propertySpec.getPathSet().add("name");
        propertySpec.setType("ManagedEntity");

        List<PropertySpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(propertySpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().addAll(propertySpecs);

        propertyFilterSpec.getObjectSet().add(new ObjectSpec());
        propertyFilterSpec.getObjectSet().get(0).setObj(serviceContent.getRootFolder());
        propertyFilterSpec.getObjectSet().get(0).setSkip(Boolean.FALSE);
        propertyFilterSpec.getObjectSet().get(0).getSelectSet().add(folderTraversalSpec);

        List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<>(1);
        propertyFilterSpecs.add(propertyFilterSpec);

        List<ObjectContent> objectContents = propertyCollectorHelper.retrieveAllProperties(propertyFilterSpecs);

        // If we get contents back. print them out.
        ObjectContent oc = null;
        ManagedObjectReference moRef = null;
        DynamicProperty pc = null;
        for (ObjectContent objectContent : objectContents) {
            oc = objectContent;
            moRef = oc.getObj();

            List<DynamicProperty> dynamicProperties = oc.getPropSet();
            log.info("Object Type : {}", moRef.getType());
            log.info("Reference Value : {}", moRef.getValue());

            if (dynamicProperties != null) {
                for (DynamicProperty dynamicProperty : dynamicProperties) {
                    pc = dynamicProperty;
                    log.info("Property Name : {}", pc.getName());
                    if (!pc.getVal().getClass().isArray()) {
                        log.info("Property Value : {}", pc.getVal());
                    } else {
                        List<Object> innerProperties = new ArrayList<>();
                        innerProperties.add(pc.getVal());
                        log.info("Val : {}", pc.getVal());
                        for (Object oval : innerProperties) {
                            if (oval.getClass().getName().contains("ManagedObjectReference")) {
                                ManagedObjectReference innerObjMoRef = (ManagedObjectReference) oval;

                                log.info("Inner Object Type : {}", innerObjMoRef.getType());
                                log.info("Inner Reference Value : {}", innerObjMoRef.getValue());
                            } else {
                                log.info("Inner Property Value : {}", oval);
                            }
                        }
                    }
                }
            }
        }
    }
}
