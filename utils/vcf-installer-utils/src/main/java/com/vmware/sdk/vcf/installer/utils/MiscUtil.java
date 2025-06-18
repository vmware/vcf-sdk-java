/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.vcf.installer.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vapi.client.ApiClient;

public class MiscUtil {

    private static final Logger log = LoggerFactory.getLogger(MiscUtil.class);

    public static String getVersionWithoutBuildNumber(ApiClient client)
            throws ExecutionException, InterruptedException {
        com.vmware.sdk.vcf.installer.v1.system.ApplianceInfo applianceInfoApi =
                client.createStub(com.vmware.sdk.vcf.installer.v1.system.ApplianceInfo.class);

        com.vmware.sdk.vcf.installer.model.ApplianceInfo applianceInfo =
                applianceInfoApi.getApplianceInfo().invoke().get();

        String applianceInfoVersion = applianceInfo.getVersion();

        String versionRegex = "(\\d+[.]\\d+[.]\\d+[.]\\d+)[.]\\d+";
        Pattern versionPattern = Pattern.compile(versionRegex);
        Matcher versionMatcher = versionPattern.matcher(applianceInfoVersion);

        if (versionMatcher.find()) {
            return versionMatcher.group(1);
        }

        throw new RuntimeException("Unable to parse version number: " + applianceInfoVersion);
    }

    /**
     * Blocks the current thread until a condition is met.
     *
     * @param maxTimeToPollInSeconds maximum time to wait
     * @param timeToSleepInBetweenPollsInSeconds how often to poll
     * @param condition If condition.get() throws an exception then polling will cease.
     * @throws InterruptedException if the current thread was interrupted before the condition was met
     */
    public static void poll(
            long maxTimeToPollInSeconds, long timeToSleepInBetweenPollsInSeconds, Supplier<Boolean> condition)
            throws InterruptedException {
        final long maxNumTimesToPoll = maxTimeToPollInSeconds / timeToSleepInBetweenPollsInSeconds;

        for (long i = 0; i < maxNumTimesToPoll; ++i) {
            if (!condition.get()) {
                log.info("Sleeping for {} seconds.", timeToSleepInBetweenPollsInSeconds);
                Thread.sleep(TimeUnit.SECONDS.toMillis(timeToSleepInBetweenPollsInSeconds));
                log.info("Polls left {}.", maxNumTimesToPoll - i - 1);
            } else {
                return;
            }
        }

        throw new RuntimeException("Polling has timed out.");
    }
}
