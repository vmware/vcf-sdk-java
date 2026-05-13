/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.nsx.basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.nsx.api.v1.TransportZones;
import com.vmware.sdk.nsx.api.v1.V1Factory;
import com.vmware.sdk.nsx.model.TransportZone;
import com.vmware.sdk.nsx.model.TransportZoneListResult;
import com.vmware.sdk.samples.nsx.helpers.NsxHelper;
import com.vmware.sdk.samples.utils.SampleCommandLineParser;

/**
 * This example shows basic CRUD (create, read, update, and delete) operations. Using one of the simplest NSX resource
 * types, a Transport Zone, we will show how the create, read, update, and delete operations are performed.
 *
 * <p>Sample prerequisites: NSX Manager instance.
 *
 * <p>APIs used:
 *
 * <ul>
 *   <li>List transport zones: GET /api/v1/transport-zones
 *   <li>Create a transport zone: POST /api/v1/transport-zones
 *   <li>Read a transport zone: POST /api/v1/transport-zones/{zone-id}
 *   <li>Update a transport zone: PUT /api/v1/transport-zones/{zone-id}
 *   <li>Delete a transport zone: DELETE /api/v1/transport-zones/{zone-id}
 * </ul>
 */
public class Crud {
    private static final Logger log = LoggerFactory.getLogger(Crud.class);
    /** REQUIRED: NSX host address or FQDN. */
    public static String nsxHostname = "hostname";
    /** REQUIRED: NSX username. */
    public static String nsxUsername = "username";
    /** REQUIRED: NSX password. */
    public static String nsxPassword = "password";

    public static void main(String[] args) {
        SampleCommandLineParser.load(Crud.class, args);
        try (NsxHelper.NsxFactory nsxFactory = new NsxHelper.NsxFactory(nsxHostname, nsxUsername, nsxPassword)) {
            V1Factory v1Factory = nsxFactory.getV1Factory();
            TransportZones transportZones = v1Factory.transportZonesService();

            // First, list all transport zones.
            TransportZoneListResult listResult =
                    transportZones.listTransportZones().invoke().get();
            log.info(
                    "Initial list of transport zones - {} zones",
                    listResult.getResults().size());

            // Create a transport tone.
            TransportZone transportZone = new TransportZone.Builder()
                    .setTransportType(TransportZone.TRANSPORT_TYPE_TRANSPORT_TYPE_OVERLAY)
                    .setDisplayName("My Transport Zone")
                    .setDescription("Transport zone for basic create/read/update/delete demo")
                    .build();

            TransportZone createResult = transportZones
                    .createTransportZone(transportZone)
                    .invoke()
                    .get();
            log.info("Transport zone created. id is {}", createResult.getId());

            // Save the id, which uniquely identifies the resource we created.
            String transportZoneId = createResult.getId();

            // Read that transport zone.
            TransportZone readResult =
                    transportZones.getTransportZone(transportZoneId).invoke().get();
            log.info("Re-read the transport zone: {}", readResult);

            // List all transport zones again. The newly created transport one will be in the list.
            listResult = transportZones.listTransportZones().invoke().get();
            log.info(
                    "Updated list of transport zones - {} zones",
                    listResult.getResults().size());

            // Update the transport zone.
            readResult.setDescription("Updated description for transport zone");
            TransportZone updateResult = transportZones
                    .updateTransportZone(transportZoneId, readResult)
                    .invoke()
                    .get();
            log.info("After updating description: {}", updateResult);

            // Update the transport zone again.
            //
            // Note that NSX insists that clients always operate on up-to-date
            // data. To enforce this, every resource in NSX has a "revision"
            // property that is automatically maintained by NSX and is
            // incremented each time the resource is updated. If a client
            // submits an update operation, and the revision property in the
            // payload provided by the client does not match the revision
            // stored on the server, another update must have happened since
            // the client last read the resource, and the client's copy is
            // therefore stale.  In this case, the server will return a 412
            // Precondition Failed error. This is intended to prevent clients
            // from clobbering each other's updates. To recover from this
            // error, the client must re-read the resource, apply any desired
            // updates, and perform another update operation.
            updateResult.setDescription("Updated description again for transport zone");
            updateResult = transportZones
                    .updateTransportZone(transportZoneId, updateResult)
                    .invoke()
                    .get();
            log.info("After updating description again: {}", updateResult);

            // Delete the transport zone.
            transportZones.deleteTransportZone(transportZoneId).invoke().get();
            log.info("Transport zone deleted.");
        } catch (Exception exception) {
            log.error("Unexpected error", exception);
        }
    }
}
