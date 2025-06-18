/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.misc.transcoder;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.utils.ssl.InsecureHostnameVerifier;
import com.vmware.sdk.utils.ssl.InsecureTrustManager;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.bindings.DynamicStructure;
import com.vmware.vapi.bindings.DynamicStructureImpl;
import com.vmware.vapi.data.DataValue;
import com.vmware.vapi.data.StructValue;
import com.vmware.vapi.protocol.common.json.JsonConverter;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;

/**
 * Demonstrates conversion of data objects from their XML representation, present in SOAP based runtimes, to their JSON
 * representation, present in JSON based runtimes, and vice versa. Conversion is done via the transcoder API, introduced
 * in version `8.0.2.0`.
 *
 * <p>The current sample utilizes a `vim.vm.ConfigSpec` managed object, present in bindings of both runtimes.
 */
public class TranscodeVmConfig {
    private static final Logger log = LoggerFactory.getLogger(TranscodeVmConfig.class);
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

    private static String version;
    private static CloseableHttpClient transcoderClient;
    // JSON based runtime serializer/deserializer
    private static final JsonConverter jsonConverter = new JsonConverter();

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(TranscodeVmConfig.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            String vimCookie = new String(client.getVimSessionProvider().get());

            if (trustStorePath == null || trustStorePath.isEmpty()) {
                setupNoSsl();
            } else {
                transcoderClient = HttpClients.createDefault();
            }

            // Negotiating API release is necessary to use in APIs
            // utilizing inheritance based polymorphism - such as transcoder API.
            // Desired version is '8.0.2.0'
            negotiateVersion(Collections.singletonList("8.0.2.0"));

            // Create SOAP vim.vm.ConfigSpec obj
            VirtualMachineConfigSpec vmConfigSpec = createVmConfigSpec();

            DynamicStructure struct = convertSoapObjToDynamicStruct(vimCookie, vmConfigSpec);
            // Demonstrate conversion in the other direction
            convertDynamicStructToSoapObj(vimCookie, struct, VirtualMachineConfigSpec.class);

            // cleanup
            transcoderClient.close();
        }
    }

    // Not recommended for production use
    protected static void setupNoSsl() throws Exception {
        // Add permissive TrustManager
        TrustManager[] trustAllCerts = new TrustManager[] {new InsecureTrustManager()};

        SSLContext sc = SSLContext.getInstance("TLS");

        // Create the session context
        SSLSessionContext sslsc = sc.getServerSessionContext();

        // Initialize the contexts; the session context takes the trust manager.
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);

        transcoderClient = HttpClients.custom()
                // Set via super class when `skipServerVerification` is enabled
                .setSSLHostnameVerifier(new InsecureHostnameVerifier())
                .setSSLContext(sc)
                .build();
    }

    /**
     * Invokes the System::Hello API, responsible for negotiating common parameters for API communication. The
     * implementation selects mutually supported version from the choices passed in the request body.
     */
    private static void negotiateVersion(List<String> clientDesiredVersions) throws Exception {
        final HttpPost req = new HttpPost("https://" + serverAddress + "/api/vcenter/system?action=hello");

        req.addHeader("Content-Type", "application/json");

        // Alternatively add org.json dependency or equivalent library
        StringBuilder jsonBodyBuilder = new StringBuilder();
        jsonBodyBuilder.append("{\"api_releases\": [");
        for (int i = 0; i < clientDesiredVersions.size(); i++) {
            if (i != 0) {
                jsonBodyBuilder.append(",");
            }
            jsonBodyBuilder.append(String.format("\"%s\"", clientDesiredVersions.get(i)));
        }
        jsonBodyBuilder.append("]}");

        req.setEntity(new StringEntity(jsonBodyBuilder.toString(), StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = transcoderClient.execute(req)) {
            String output = EntityUtils.toString(response.getEntity());
            // Custom error-prone version extraction
            // Utilize org.json or equivalent for JSON handling
            version = output.split(":")[1].replaceAll("\"|}", "");
            log.info("Version: {}", version);
        }
    }

    /**
     * Transcodes and validates the integrity of a JSON or XML serialized data object.
     *
     * <p>Transcoding is available from JSON or XML to JSON or XML for both cases.
     *
     * <p>Transcoding to different encoding types is useful when utilizing the same data objects in a program involving
     * SOAP and JSON based stacks/bindings.
     */
    private static String transcode(String vimCookie, String data, boolean toJson) throws Exception {
        final HttpPost req = new HttpPost("https://" + serverAddress + "/sdk/vim25/" + version + "/transcoder");
        req.addHeader("Content-Type", toJson ? "application/xml" : "application/json");
        req.addHeader("Accept", toJson ? "application/json" : "application/xml");
        req.addHeader("vmware-api-session-id", vimCookie);
        req.setEntity(new StringEntity(data, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = transcoderClient.execute(req)) {
            return EntityUtils.toString(response.getEntity());
        }
    }

    /** Creates a `vim.vm.ConfigSpec` data object with arbitrarily populated fields. */
    private static VirtualMachineConfigSpec createVmConfigSpec() {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        vmConfigSpec.setName("test-vm");
        vmConfigSpec.setMemoryMB(4L);
        vmConfigSpec.setGuestId("guest");
        vmConfigSpec.setAnnotation("Sample");
        vmConfigSpec.setNumCPUs(1);

        VirtualMachineFileInfo files = new VirtualMachineFileInfo();
        files.setVmPathName("[datastore1]");

        vmConfigSpec.setFiles(files);

        return vmConfigSpec;
    }

    private static String serializeToXml(Object managedObj) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(managedObj.getClass());
        Marshaller marshaller = context.createMarshaller();

        // To format XML
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // If we DO NOT have JAXB annotated class
        JAXBElement<Object> jaxbElement = new JAXBElement<>(new QName("urn:vim25", "obj"), Object.class, managedObj);

        StringWriter writer = new StringWriter();

        marshaller.marshal(jaxbElement, writer);

        return writer.toString();
    }

    private static <T> T deserializeFromXml(String xml, Class<T> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Source source = new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        JAXBElement<T> root = context.createUnmarshaller().unmarshal(source, clazz);
        return root.getValue();
    }

    private static DynamicStructure convertSoapObjToDynamicStruct(String vimCookie, Object soapObj) throws Exception {
        // Serialize SOAP object to XML
        String xml = serializeToXml(soapObj);

        // Transcode XML to JSON
        String json = transcode(vimCookie, xml, true);
        log.info(json);

        // Deserialize JSON to a StructValue
        byte[] buf = json.getBytes(StandardCharsets.UTF_8);
        InputStream input = new ByteArrayInputStream(buf);
        DataValue dataValue = jsonConverter.toDataValue(input);

        // Convert StructValue to DynamicStructure
        DynamicStructure struct = new DynamicStructureImpl((StructValue) dataValue);
        log.info("Struct converted: {}", struct.getClass().getName());

        return struct;
    }

    /**
     * Converts a DynamicStructure to its equivalent SOAP type {@code T}.
     *
     * <p>Passing an invalid type results in XML conversion errors.
     */
    private static <T> void convertDynamicStructToSoapObj(String vimCookie, DynamicStructure struct, Class<T> clazz)
            throws Exception {
        // Serialize DynamicStructure into JSON
        String json = jsonConverter.fromDataValue(struct._getDataValue());

        // Transcode JSON to XML
        String xml = transcode(vimCookie, json, false);
        System.out.println(xml);

        T soapObj = deserializeFromXml(xml, clazz);
        log.info("Struct converted: {}", soapObj.getClass().getName());
    }
}
