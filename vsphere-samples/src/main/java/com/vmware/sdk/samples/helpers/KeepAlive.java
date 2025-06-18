/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VimPortType;

/**
 * This is a keep-alive utility class. It will keep an instance of a connection alive by polling the "currentTime"
 * method on the remote Host or vCenter that the supplied connection and VimPortType were talking to.
 *
 * @see com.vmware.vim25.VimPortType
 */
public class KeepAlive implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(KeepAlive.class);
    private static final Long DEFAULT_INTERVAL = 300000L;
    private final boolean verbose = Boolean.parseBoolean(System.getProperty("keep-alive.verbose", "false"));
    private volatile Boolean running;
    private final Long interval;
    private final VimPortType vimPort;
    private final ManagedObjectReference serviceInstanceReference;

    /**
     * this class is immutable and acts on the supplied vimPort and serviceInstanceReference the default interval is set
     * to 300000 milliseconds
     *
     * @param vimPort the port providing the "currentTime" method
     * @param serviceInstanceReference service reference to be passed to the method
     */
    public KeepAlive(final VimPortType vimPort, final ManagedObjectReference serviceInstanceReference) {
        this(vimPort, serviceInstanceReference, DEFAULT_INTERVAL);
    }

    /**
     * builds an instance of this object
     *
     * @param vimPort the port providing the "currentTime" method
     * @param serviceInstanceReference service reference to be passed to the method
     * @param interval the interval in milliseconds between calls to "currentTime" \
     */
    public KeepAlive(
            final VimPortType vimPort, final ManagedObjectReference serviceInstanceReference, final Long interval) {
        this.vimPort = vimPort;
        this.serviceInstanceReference = serviceInstanceReference;
        this.interval = interval;
        this.running = Boolean.TRUE;
    }

    /** kicks off a thread that will call the "keep alive" method on the connection instance */
    public void keepAlive() {
        try {
            run(vimPort, serviceInstanceReference);
        } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            log.error("RuntimeFaultFaultMsg: ", runtimeFaultFaultMsg);
        } catch (Exception e) {
            stop();
        }
    }

    /**
     * calls "currentTime" against the supplied objects
     *
     * @param vimPort the port providing the "currentTime" method
     * @param serviceInstanceRef service reference to be passed to the method
     * @throws RuntimeFaultFaultMsg if there is an error during the call
     */
    public static void run(final VimPortType vimPort, final ManagedObjectReference serviceInstanceRef)
            throws RuntimeFaultFaultMsg {
        vimPort.currentTime(serviceInstanceRef);
    }

    /** @return true if the embedded thread is running */
    public boolean isRunning() {
        return running;
    }

    /** signals the embedded thread to stop */
    public void stop() {
        if (verbose) {
            log.info("keep alive stopped");
        }
        running = false;
    }

    /** starts a keep-alive thread which will call keepAlive then sleep for the interval */
    @Override
    public void run() {
        running = true;
        try {
            while (isRunning()) {
                if (verbose) {
                    log.info("keep alive");
                }
                keepAlive();
                Thread.sleep(interval);
            }
        } catch (Throwable t) {
            stop();
        }
    }

    /**
     * Returns a thread you can start to run a keep alive on your connection. You supply it with your copy of the
     * vimPort and serviceInstanceRef to ping. Call start on the thread when you need to start the keep-alive.
     *
     * @param vimPort the port providing the "currentTime" method
     * @param serviceInstanceRef service reference to be passed to the method
     * @return the new thread
     */
    public static Thread keepAlive(VimPortType vimPort, ManagedObjectReference serviceInstanceRef) {
        return keepAlive(vimPort, serviceInstanceRef, DEFAULT_INTERVAL);
    }

    /**
     * constructs a new embedded thread to keep alive
     *
     * @param vimPort the port providing the "currentTime" method
     * @param serviceInstanceRef service reference to be passed to the method
     * @param interval the interval in milliseconds between calls to "currentTime"
     * @return the new thread
     */
    public static Thread keepAlive(VimPortType vimPort, ManagedObjectReference serviceInstanceRef, Long interval) {
        return new Thread(new KeepAlive(vimPort, serviceInstanceRef, interval));
    }
}
