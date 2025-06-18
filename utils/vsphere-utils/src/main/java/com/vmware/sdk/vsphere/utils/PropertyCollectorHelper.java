/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vsphere.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ManagedObjectType;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.WaitOptions;

/**
 * This util class provides helper methods for the core PropertyCollector service functionality - monitor and retrieve
 * information about managed objects, traversal and fetching properties.
 */
public class PropertyCollectorHelper {

    private static final Logger log = LoggerFactory.getLogger(PropertyCollectorHelper.class);
    private final VimPortType vimPort;
    private final ServiceContent serviceContent;

    /**
     * A default value for {@link RetrieveOptions#setMaxObjects(Integer)} which is the maximum number of
     * {@link ObjectContent} data objects that should be returned in a single result from RetrievePropertiesEx.
     */
    private static final int DEFAULT_CHUNK_SIZE = 100;

    /**
     * This constructor uses already created and configured {@code vimPort} and {@code serviceContent}.
     *
     * @param vimPort the VIM port, which is fully configured and authenticated
     * @param serviceContent the already retrieved {@link ServiceContent} data object
     * @see VimClient#getVimPort()
     * @see VimClient#getVimServiceContent()
     */
    public PropertyCollectorHelper(VimPortType vimPort, ServiceContent serviceContent) {
        this.vimPort = vimPort;
        this.serviceContent = serviceContent;
    }

    /**
     * This constructor is going to fetch the {@link ServiceContent} using the {@code vimPort}.
     *
     * @param vimPort the VIM port, which is fully configured and authenticated
     * @see VimPortType#retrieveServiceContent(ManagedObjectReference)
     */
    public PropertyCollectorHelper(VimPortType vimPort) {
        this.vimPort = vimPort;
        try {
            ManagedObjectReference serviceInstance = new ManagedObjectReference();
            serviceInstance.setType("ServiceInstance");
            serviceInstance.setValue("ServiceInstance");
            this.serviceContent = vimPort.retrieveServiceContent(serviceInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method returns a boolean value specifying whether the Task is succeeded or failed.
     *
     * @param task {@link ManagedObjectReference} representing the Task
     * @return boolean value representing the Task result
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     * @throws InvalidCollectorVersionFaultMsg If the version of the property collector is not supported
     */
    public boolean awaitTaskCompletion(ManagedObjectReference task)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

        boolean taskSucceeded = false;

        // info has a property - state for state of the task
        Object[] result = awaitManagedObjectUpdates(
                task, new String[] {"info.state", "info.error"}, new String[] {"state"}, new Object[][] {
                    new Object[] {TaskInfoState.SUCCESS, TaskInfoState.ERROR}
                });

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            taskSucceeded = true;
        }
        if (result[1] instanceof LocalizedMethodFault) {
            throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
        }
        return taskSucceeded;
    }

    /**
     * Handle Updates for a single object. Waits until expected values of the properties to check are reached. Destroys
     * the ObjectFilter when done.
     *
     * @param objMoRef {@link ManagedObjectReference} of the Object to wait for
     * @param filterProperties properties list to filter. Example value: "info.state", "info.error"
     * @param endWaitProperties properties list to check for expected values
     * @param expectedValues values for the properties to end the wait
     * @param maxWaitSeconds the number of seconds the PropertyCollector should wait before returning null. An unset
     *     value causes {@link VimPortType#waitForUpdatesEx} to wait as long as possible for updates. A value of 0
     *     causes {@link VimPortType#waitForUpdatesEx} to do one update calculation and return any results. A positive
     *     value causes {@link VimPortType#waitForUpdatesEx} to return null if no updates are available within the
     *     specified number of seconds. The choice of a positive value often depends on the client communication stack -
     *     it is recommended to choose a duration that is shorter than the HTTP request timeout of the client, to avoid
     *     any potential timeouts or connection issues. A negative value is illegal
     * @return true indicating expected values were met, and false otherwise
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     * @throws InvalidCollectorVersionFaultMsg If the version of the property collector is not supported
     */
    public Object[] awaitManagedObjectUpdates(
            ManagedObjectReference objMoRef,
            String[] filterProperties,
            String[] endWaitProperties,
            Object[][] expectedValues,
            Integer maxWaitSeconds)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

        log.debug("Awaiting updates for {} - {}", objMoRef.getType(), objMoRef.getValue());

        // version string is initially null
        String version = "";
        Object[] endValues = new Object[endWaitProperties.length];
        Object[] filterValues = new Object[filterProperties.length];
        String stateValue = null;

        PropertyFilterSpec filterSpec = createPropertyFilterSpec(objMoRef, null, Boolean.FALSE, filterProperties);

        ManagedObjectReference filterSpecRef =
                vimPort.createFilter(serviceContent.getPropertyCollector(), filterSpec, true);

        boolean reached = false;

        UpdateSet updateset = null;
        List<PropertyFilterUpdate> filterUpdates = null;
        List<ObjectUpdate> objectUpdates = null;
        List<PropertyChange> propertyChanges = null;

        WaitOptions waitOptions = new WaitOptions();
        waitOptions.setMaxWaitSeconds(maxWaitSeconds);
        while (!reached) {
            updateset = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), version, waitOptions);
            if (updateset == null || updateset.getFilterSet() == null) {
                log.debug("No updates present, waiting for further updates...");
                continue;
            }
            version = updateset.getVersion();

            // Make this code more general purpose when PropCol changes later.
            filterUpdates = updateset.getFilterSet();

            for (PropertyFilterUpdate filterUpdate : filterUpdates) {
                objectUpdates = filterUpdate.getObjectSet();
                for (ObjectUpdate objectUpdate : objectUpdates) {
                    // TODO: Handle all "kind"s of updates.
                    if (objectUpdate.getKind() == ObjectUpdateKind.MODIFY
                            || objectUpdate.getKind() == ObjectUpdateKind.ENTER
                            || objectUpdate.getKind() == ObjectUpdateKind.LEAVE) {
                        propertyChanges = objectUpdate.getChangeSet();
                        for (PropertyChange propertyChange : propertyChanges) {
                            updateValues(endWaitProperties, endValues, propertyChange);
                            updateValues(filterProperties, filterValues, propertyChange);
                        }
                    }
                }
            }

            Object expectedValue = null;
            // Check if the expected values have been reached and exit the loop
            // if done.
            // Also exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endValues.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedValues[chgi].length && !reached; vali++) {
                    expectedValue = expectedValues[chgi][vali];
                    if (endValues[chgi] == null) {
                        // Do Nothing
                    } else if (endValues[chgi].toString().contains("val: null")) {
                        // Due to some issue in JAX-WS De-serialization getting the information from
                        // the nodes
                        Element stateElement = (Element) endValues[chgi];
                        if (stateElement != null && stateElement.getFirstChild() != null) {
                            stateValue = stateElement.getFirstChild().getTextContent();
                            reached = expectedValue.toString().equalsIgnoreCase(stateValue) || reached;
                        }
                    } else {
                        expectedValue = expectedValues[chgi][vali];
                        reached = expectedValue.equals(endValues[chgi]) || reached;
                        stateValue = "filtervals";
                    }
                }
            }
        }
        Object[] returnValue = null;
        // Destroy the filter when we are done.
        try {
            vimPort.destroyPropertyFilter(filterSpecRef);
        } catch (RuntimeFaultFaultMsg e) {
            log.error("Error destroying property filter: {}", filterSpecRef, e);
        }
        if (stateValue != null) {
            if (stateValue.equalsIgnoreCase("ready")) {
                returnValue = new Object[] {HttpNfcLeaseState.READY};
            }
            if (stateValue.equalsIgnoreCase("error")) {
                returnValue = new Object[] {HttpNfcLeaseState.ERROR};
            }
            if (stateValue.equals("filtervals")) {
                returnValue = filterValues;
            }
        } else {
            returnValue = new Object[] {HttpNfcLeaseState.ERROR};
        }
        return returnValue;
    }

    /**
     * Calls {@link PropertyCollectorHelper#awaitManagedObjectUpdates} with {@code maxWaitSeconds} set to 20 seconds.
     *
     * @param objMoRef {@link ManagedObjectReference} of the Object to wait for
     * @param filterProperties properties list to filter. Example value: "info.state", "info.error"
     * @param endWaitProperties properties list to check for expected values
     * @param expectedValues values for the properties to end the wait
     * @return true indicating expected values were met, and false otherwise
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     * @throws InvalidCollectorVersionFaultMsg If the version of the property collector is not supported
     */
    public Object[] awaitManagedObjectUpdates(
            ManagedObjectReference objMoRef,
            String[] filterProperties,
            String[] endWaitProperties,
            Object[][] expectedValues)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        return awaitManagedObjectUpdates(objMoRef, filterProperties, endWaitProperties, expectedValues, 20);
    }

    /**
     * Updates the values of the given properties based on the provided property change. If the property change
     * operation is {@link PropertyChangeOp#REMOVE}, the corresponding value is set to an empty string. Otherwise, the
     * value is updated with the new value from the property change.
     *
     * @param properties an array of String representing the names of the properties to update
     * @param values an array of Objects representing the current values of the properties
     * @param propertyChange the {@link PropertyChange} object containing the information about the property change
     */
    private void updateValues(String[] properties, Object[] values, PropertyChange propertyChange) {
        for (int findi = 0; findi < properties.length; findi++) {
            if (propertyChange.getName().lastIndexOf(properties[findi]) >= 0) {
                if (propertyChange.getOp() == PropertyChangeOp.REMOVE) {
                    values[findi] = "";
                } else {
                    values[findi] = propertyChange.getVal();
                }
            }
        }
    }

    /**
     * Retrieves the specified properties of the specified managed objects. Returns the raw {@link RetrieveResult}
     * object for the provided container, filtered on properties list.
     *
     * @param containerView view of a container object
     * @param chunkSize the maximum number of {@link ObjectContent} data objects that should be returned in a single
     *     result from RetrievePropertiesEx. A value less than or equal to 0 is illegal.
     * @param moType type of the managed object to filter for. See {@link ManagedObjectType}
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return {@link RetrieveResult} for this query
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public RetrieveResult retrieveContainerView(
            ManagedObjectReference containerView, int chunkSize, ManagedObjectType moType, String... properties)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        List<PropertyFilterSpec> propertyFilterSpecs =
                createPropertyFilterSpecsForContainerView(containerView, moType, properties);

        RetrieveOptions retrieveOptions = createRetrieveOptions(chunkSize);

        return vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(), propertyFilterSpecs, retrieveOptions);
    }

    /**
     * Creates a view of the specified container object.
     *
     * @param container container to look in
     * @param moType type of the managed object to filter for. See {@link ManagedObjectType}
     * @return ManagedObjectReference of the created view
     * @throws RuntimeFaultFaultMsg If the view could not be created
     */
    public ManagedObjectReference createContainerView(ManagedObjectReference container, ManagedObjectType moType)
            throws RuntimeFaultFaultMsg {
        ManagedObjectReference viewManager = serviceContent.getViewManager();
        return vimPort.createContainerView(viewManager, container, Collections.singletonList(moType.value()), true);
    }

    private void destroyContainerView(ManagedObjectReference containerView) throws RuntimeFaultFaultMsg {
        if (containerView != null) {
            vimPort.destroyView(containerView);
        }
    }

    /**
     * Retrieves a subset of properties for objects identified by their Managed Object References within a container.
     * Property names can be found in the "vSphere Web Services API" documentation at developer portal under the
     * different Managed Object Types e.g. VirtualMachine, HostSystem etc. Property paths are specified with a dot
     * notation.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param container {@link ManagedObjectReference} of the container to begin the search from
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @param chunkSize the maximum number of {@link ObjectContent} data objects that should be returned in a single
     *     result from RetrievePropertiesEx. A value less than or equal to 0 is illegal.
     * @return Map of {@link ManagedObjectReference} and Map of name value pair of properties requested of the managed
     *     objects present. If none exist, then empty Map is returned
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs during the search
     */
    public Map<ManagedObjectReference, Map<String, Object>> getObjectProperties(
            ManagedObjectReference container, ManagedObjectType moType, int chunkSize, String... properties)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        Map<ManagedObjectReference, Map<String, Object>> entityMoRefMap = new HashMap<>();
        ManagedObjectReference containerView = createContainerView(container, moType);

        try {
            RetrieveResult retrieveResult = retrieveContainerView(containerView, chunkSize, moType, properties);

            iterateObjects(retrieveResult, oc -> {
                Map<String, Object> propertyMap = new HashMap<>();
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        propertyMap.put(dp.getName(), dp.getVal());
                    }
                }
                entityMoRefMap.put(oc.getObj(), propertyMap);
                return false;
            });
        } finally {
            try {
                destroyContainerView(containerView);
            } catch (RuntimeFaultFaultMsg e) {
                log.error("Error destroying container view", e);
            }
        }

        return entityMoRefMap;
    }

    /**
     * Retrieves a subset of properties for objects identified by their Managed Object References within a container.
     * This method uses the default {@link RetrieveOptions}. Property names can be found in the "vSphere Web Services
     * API" documentation at developer portal under the different Managed Object Types e.g. VirtualMachine, HostSystem
     * etc. Property paths are specified with a dot notation.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param container {@link ManagedObjectReference} of the container to begin the search from
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return Map of {@link ManagedObjectReference} and Map of name value pair of properties requested of the managed
     *     objects present. If none exist, then empty Map is returned
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs during the search
     */
    public Map<ManagedObjectReference, Map<String, Object>> getObjectProperties(
            ManagedObjectReference container, ManagedObjectType moType, String... properties)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return getObjectProperties(container, moType, DEFAULT_CHUNK_SIZE, properties);
    }

    /**
     * Retrieves the identifiers of all objects of the specified type within the given container.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param container {@link ManagedObjectReference} of the container to begin the search from
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @param chunkSize the maximum number of {@link ObjectContent} data objects that should be returned in a single
     *     result from RetrievePropertiesEx. A value less than or equal to 0 is illegal.
     * @return Map of name and a list with the Managed Object Reference of the managed objects present for that name. If
     *     none exist, then empty Map is returned
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs during the search
     */
    public Map<String, List<ManagedObjectReference>> getObjects(
            ManagedObjectReference container, ManagedObjectType moType, int chunkSize)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        ManagedObjectReference containerView = createContainerView(container, moType);

        Map<String, List<ManagedObjectReference>> result = new HashMap<>();
        try {
            RetrieveResult retrieveResult = retrieveContainerView(containerView, chunkSize, moType, "name");

            iterateObjects(retrieveResult, oc -> {
                if (!oc.getPropSet().isEmpty()) {
                    // there should be exactly 1 element because there is only one PropertyFilterSpec ("name")
                    String key = (String) oc.getPropSet().get(0).getVal();
                    result.computeIfAbsent(key, k -> new ArrayList<>()).add(oc.getObj());
                }
                return false;
            });
        } finally {
            try {
                destroyContainerView(containerView);
            } catch (RuntimeFaultFaultMsg e) {
                log.error("Error destroying container view", e);
            }
        }

        return result;
    }

    /**
     * Retrieves the identifiers of all objects of the specified type within the given container. This method uses the
     * default {@link RetrieveOptions}.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param container {@link ManagedObjectReference} of the container to begin the search from
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @return Map of name and a list with the Managed Object Reference of the managed objects present for that name. If
     *     none exist, then empty Map is returned
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs during the search
     */
    public Map<String, List<ManagedObjectReference>> getObjects(
            ManagedObjectReference container, ManagedObjectType moType)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return getObjects(container, moType, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Gets the {@link ManagedObjectReference} of an entity by its name and {@link ManagedObjectType}, starting the
     * search from the root folder.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param name name of the entity to be searched
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @return {@link ManagedObjectReference} of the entity
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public ManagedObjectReference getMoRefByName(String name, ManagedObjectType moType)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return getMoRefByName(serviceContent.getRootFolder(), DEFAULT_CHUNK_SIZE, name, moType);
    }

    /**
     * Gets the {@link ManagedObjectReference} of an entity by its name and {@link ManagedObjectType}, starting the
     * search from the specified container.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * inventory. To improve performance, provide a specific container that is as close to the desired managed object as
     * possible.
     *
     * @param name name of the entity to be searched
     * @param container The container to look in and from which to start the search for the entity (defaults to the root
     *     folder)
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @return {@link ManagedObjectReference} of the entity
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public ManagedObjectReference getMoRefByName(
            ManagedObjectReference container, String name, ManagedObjectType moType)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return getMoRefByName(container, DEFAULT_CHUNK_SIZE, name, moType);
    }

    /**
     * Gets the {@link ManagedObjectReference} of an entity by its name and {@link ManagedObjectType}, using a specific
     * container and retrieve options.
     *
     * <p>Note that starting the search from the root folder may result in performance issues as it can fetch the entire
     * * inventory. To improve performance, provide a specific container that is as close to the desired managed object
     * as * possible.
     *
     * @param container container to look in and from which to start the search for the entity
     * @param chunkSize the maximum number of {@link ObjectContent} data objects that should be returned in a single
     *     result from RetrievePropertiesEx. A value less than or equal to 0 is illegal.
     * @param name name of the entity to be searched
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @return {@link ManagedObjectReference} of the entity
     * @throws InvalidPropertyFaultMsg If the property does not exist for this managed object type
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public ManagedObjectReference getMoRefByName(
            ManagedObjectReference container, int chunkSize, String name, ManagedObjectType moType)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ManagedObjectReference containerView = createContainerView(container, moType);

        AtomicReference<ManagedObjectReference> returnValue = new AtomicReference<>();
        try {
            RetrieveResult retrieveResult = retrieveContainerView(containerView, chunkSize, moType, "name");

            iterateObjects(retrieveResult, oc -> {
                ManagedObjectReference moRef = oc.getObj();
                String managedObjectName = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        managedObjectName = (String) dp.getVal();
                    }
                }
                if (managedObjectName != null && managedObjectName.equals(name)) {
                    returnValue.set(moRef);
                    return true;
                }
                return false;
            });

        } finally {
            try {
                destroyContainerView(containerView);
            } catch (RuntimeFaultFaultMsg e) {
                log.error("Error destroying container view", e);
            }
        }

        return returnValue.get();
    }

    /**
     * Method to retrieve properties of a managed object. Property names can be found in the "vSphere Web Services API"
     * documentation at developer portal under the different Managed Object Types e.g. VirtualMachine, HostSystem etc.
     * Property paths are specified with a dot notation.
     *
     * @param entityMoRef {@link ManagedObjectReference} of the entity
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return Map of the property name and its corresponding value
     * @throws InvalidPropertyFaultMsg If a property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public Map<String, Object> fetchProperties(ManagedObjectReference entityMoRef, String... properties)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        final HashMap<String, Object> propertyValuesMap = new HashMap<>();

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        PropertyFilterSpec propertyFilterSpec = createPropertyFilterSpec(entityMoRef, Boolean.FALSE, null, properties);

        RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(),
                List.of(propertyFilterSpec),
                createRetrieveOptions(DEFAULT_CHUNK_SIZE));

        iterateObjects(retrieveResult, oc -> {
            List<DynamicProperty> dps = oc.getPropSet();
            for (DynamicProperty dp : dps) {
                propertyValuesMap.put(dp.getName(), dp.getVal());
            }
            return false;
        });

        return propertyValuesMap;
    }

    /**
     * Retrieves a subset of properties for a given set of objects. Property names can be found in the "vSphere Web
     * Services API" documentation at developer portal under the different Managed Object Types e.g. VirtualMachine,
     * HostSystem etc. Property paths are specified with a dot notation.
     *
     * @param entityMoRefs list of {@link ManagedObjectReference} for which the properties need to be retrieved
     * @param properties comma separated properties that need to be retrieved for all the {@link ManagedObjectReference}
     *     passed. Example value: "summary.config.guestId", "name"
     * @return Map of {@link ManagedObjectReference} and their corresponding name value pair of properties
     * @throws InvalidPropertyFaultMsg If a property does not exist for these entities
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    public Map<ManagedObjectReference, Map<String, Object>> fetchProperties(
            List<ManagedObjectReference> entityMoRefs, String... properties)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        Map<ManagedObjectReference, Map<String, Object>> propertyValuesMap = new HashMap<>();

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        Map<String, String> typesCovered = new HashMap<>();

        for (ManagedObjectReference moRef : entityMoRefs) {
            if (!typesCovered.containsKey(moRef.getType())) {
                // Create & add new property Spec
                PropertySpec propertySpec = createPropertySpec(moRef.getType(), false, properties);

                propertyFilterSpec.getPropSet().add(propertySpec);

                typesCovered.put(moRef.getType(), "");
            }
            // Now create & add Object Spec
            ObjectSpec objectSpec = new ObjectSpec();
            objectSpec.setObj(moRef);

            propertyFilterSpec.getObjectSet().add(objectSpec);
        }

        RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(),
                List.of(propertyFilterSpec),
                createRetrieveOptions(DEFAULT_CHUNK_SIZE));

        iterateObjects(retrieveResult, oc -> {
            Map<String, Object> propMap = new HashMap<>();
            List<DynamicProperty> dps = oc.getPropSet();
            for (DynamicProperty dp : dps) {
                propMap.put(dp.getName(), dp.getVal());
            }

            propertyValuesMap.put(oc.getObj(), propMap);
            return false;
        });

        return propertyValuesMap;
    }

    /**
     * Method to retrieve a single property of a managed object. Property names can be found in the "vSphere Web
     * Services API" documentation at developer portal under the different Managed Object Types e.g. VirtualMachine,
     * HostSystem etc. Property paths are specified with a dot notation.
     *
     * <p>Example: To find the number of virtual CPUs present in this virtual machine, the property must be set to
     * "config.hardware.numCPU".
     *
     * @param entityMoRef {@link ManagedObjectReference} of the entity
     * @param property the property to be looked up. Example value: "config.hardware.numCPU"
     * @param <T> type of the property value to be retrieved
     * @return The value of the retrieved property
     * @throws InvalidPropertyFaultMsg If a property does not exist for this managed object
     * @throws RuntimeFaultFaultMsg If any other error occurs while executing the query
     */
    @SuppressWarnings("unchecked")
    public <T> T fetch(ManagedObjectReference entityMoRef, String property)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        PropertyFilterSpec propertyFilterSpec = createPropertyFilterSpec(entityMoRef, Boolean.FALSE, null, property);

        RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(),
                List.of(propertyFilterSpec),
                createRetrieveOptions(DEFAULT_CHUNK_SIZE));

        AtomicReference<T> propertyValue = new AtomicReference<>();
        iterateObjects(retrieveResult, oc -> {
            List<DynamicProperty> dps = oc.getPropSet();
            if (!dps.isEmpty()) {
                propertyValue.set((T) dps.get(0).getVal());
            }
            return true;
        });

        return propertyValue.get();
    }

    /**
     * Retrieves a list with {@link ObjectContent} for all the specified filter properties.
     *
     * @param propertyFilterSpecs list with filter properties to retrieve
     * @return {@link ObjectContent} list of all retrieved properties
     * @throws InvalidPropertyFaultMsg If a property does not exist for the managed objects in this RetrieveResult
     * @throws RuntimeFaultFaultMsg If any other error occurs
     */
    public List<ObjectContent> retrieveAllProperties(List<PropertyFilterSpec> propertyFilterSpecs)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        List<ObjectContent> objectContentList = new ArrayList<>();

        RetrieveResult retrieveResult = vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(), propertyFilterSpecs, createRetrieveOptions(DEFAULT_CHUNK_SIZE));

        iterateObjects(retrieveResult, oc -> {
            objectContentList.add(oc);
            return false;
        });

        return objectContentList;
    }

    /**
     * This method is used to retrieve properties of multiple objects from vSphere. It continues to fetch properties of
     * objects until there are no more objects to fetch or the user requests to stop fetching.
     *
     * @param retrieveResult the {@link RetrieveResult} object containing the objects to retrieve properties for and the
     *     token for continuing the retrieval
     * @param function a function that takes an {@link ObjectContent} object and returns a boolean indicating whether to
     *     stop fetching properties and terminate the iteration
     * @throws InvalidPropertyFaultMsg If a property does not exist for the managed objects in this RetrieveResult
     * @throws RuntimeFaultFaultMsg If any other error occurs
     */
    public void iterateObjects(RetrieveResult retrieveResult, Function<ObjectContent, Boolean> function)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        while (retrieveResult != null) {

            boolean stopFetching = true;
            for (ObjectContent oc : retrieveResult.getObjects()) {
                stopFetching = function.apply(oc);

                if (stopFetching) {
                    break;
                }
            }

            if (stopFetching && retrieveResult.getToken() != null) {
                try {
                    vimPort.cancelRetrievePropertiesEx(
                            serviceContent.getPropertyCollector(), retrieveResult.getToken());
                } catch (Exception e) {
                    log.warn("Error cancelling property retrieval", e);
                }
                break; // the user wants to stop fetching items
            }

            if (retrieveResult.getToken() == null) {
                break; // there are no more items to fetch
            }

            retrieveResult = vimPort.continueRetrievePropertiesEx(
                    serviceContent.getPropertyCollector(), retrieveResult.getToken());
        }
    }

    /**
     * Creates {@link PropertySpec} for the specified Managed Object type and set of property paths.
     *
     * @param moType type of the managed entity that needs to be searched. See {@link ManagedObjectType}
     * @param setAll specifies whether or not all properties of the object are read. If this property is set to true,
     *     the pathSet property is ignored
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return {@link PropertySpec} for the specified Managed Object type and properties
     */
    public PropertySpec createPropertySpec(String moType, Boolean setAll, String... properties) {
        PropertySpec propertySpec = new PropertySpec();
        propertySpec.setAll(setAll);
        propertySpec.setType(moType);
        propertySpec.getPathSet().addAll(List.of(properties));
        return propertySpec;
    }

    /**
     * * Creates {@link PropertyFilterSpec} for the specified managed object, specifying the property data that is
     * included in a filter.
     *
     * @param entityMoRef {@link ManagedObjectReference} of the entity
     * @param setAll specifies whether or not all properties of the object are read. If this property is set to true,
     *     the {@code pathSet} property is ignored
     * @param setSkip flag to specify whether or not to report this managed object's properties. If the flag is true,
     *     the filter will not report this managed object's properties.
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return property filter specification for the managed object
     */
    public PropertyFilterSpec createPropertyFilterSpec(
            ManagedObjectReference entityMoRef, Boolean setAll, Boolean setSkip, String... properties) {
        PropertySpec propertySpec = createPropertySpec(entityMoRef.getType(), setAll, properties);

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(entityMoRef);
        objectSpec.setSkip(setSkip);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        return propertyFilterSpec;
    }

    /**
     * Creates a list of {@link PropertyFilterSpec} using a container view for traversal.
     *
     * @param containerView view of a container object
     * @param moType type of the managed object to filter for. See {@link ManagedObjectType}
     * @param properties comma separated properties to be looked up. Example value: "summary.config.guestId", "name"
     * @return list of a {@link PropertyFilterSpec} for the managed object type
     */
    public List<PropertyFilterSpec> createPropertyFilterSpecsForContainerView(
            ManagedObjectReference containerView, ManagedObjectType moType, String... properties) {

        PropertySpec propertySpec = createPropertySpec(moType.value(), false, properties);

        TraversalSpec traversalSpec = new TraversalSpec();
        traversalSpec.setName("view");
        traversalSpec.setPath("view");
        traversalSpec.setSkip(Boolean.FALSE);
        traversalSpec.setType("ContainerView");

        ObjectSpec objectSpec = new ObjectSpec();
        objectSpec.setObj(containerView);
        objectSpec.setSkip(Boolean.TRUE);
        objectSpec.getSelectSet().add(traversalSpec);

        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
        propertyFilterSpec.getPropSet().add(propertySpec);
        propertyFilterSpec.getObjectSet().add(objectSpec);

        return List.of(propertyFilterSpec);
    }

    /**
     * Creates a {@link RetrieveOptions} with a custom number of objects that should be returned in a single result.
     *
     * @param chunkSize the maximum number of {@link ObjectContent} data objects that should be returned in a single
     *     result from RetrievePropertiesEx. A value less than or equal to 0 is illegal.
     * @return {@link RetrieveOptions} with the maximum number of data objects to be returned
     */
    private static RetrieveOptions createRetrieveOptions(int chunkSize) {
        RetrieveOptions retrieveOptions = new RetrieveOptions();
        retrieveOptions.setMaxObjects(chunkSize);
        return retrieveOptions;
    }
}
