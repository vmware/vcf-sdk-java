# Java SDK Migration Guide

1. [Preface](#preface)
2. [Getting started](#getting-started)
   1. [Key SDK concepts](#key-sdk-concepts)
   2. [Migrating existing applications to the new SDK](#migrating-existing-applications-to-the-new-sdk)
   3. [Build system changes](#build-system-changes)
   4. [Application code changes](#application-code-changes)
      1. [Common client initialization](#common-client-initialization)
         1. [vCenter](#vcenter)
         2. [STS (ssoclient)](#sts-ssoclient)
         3. [SMS / PBM / VSLM](#sms-pbm-vslm)
         4. [vSAN](#vsan)
      2. [Advanced client initialization](#advanced-client-initialization)
      3. [Using custom trust store](#using-custom-trust-store)
      4. [Insecure clients](#insecure-clients)
      5. [Migrating  business logic to the new utility code](#migrating-business-logic-to-the-new-utility-code)

# Preface

The VCF 9.0 Java SDK comes with various improvements in different areas. This document provides a high-level overview of the changes and guidelines for application developers \- how to use the new SDK and how to migrate existing applications from older SDK versions.

Prior to the VCF 9.0 release, there were 3 SDKs:

- [vSphere Automation SDK](https://github.com/vmware/vsphere-automation-sdk-java)  
- [vSphere Management SDK](https://developer.broadcom.com/sdks/vsphere-management-sdk/latest)  
- [vSAN Management SDK](https://developer.broadcom.com/sdks/vsan-management-sdk-for-java/latest)

Historically these SDKs contained jars with bindings for the respective service, samples and utility code.

Integrating these SDKs into a customer application required the developer to:

* download the SDK locally  
  * for the vSphere Automation SDK \- download one or more jars from the “lib” folder of the GitHub repository  
  * for the vSphere Management SDK  
    * download the VMware-vSphere-SDK-\<version\>-\<build-number\>.zip and extract its contents  
    * copy the jars from libs/jaxws-ri  
    * copy the jars from \<sdk\>/java/JAXWS/lib  
  * for the vSphere vSAN Management SDK  
    * follow the setup for vSphere Management SDK and copy the jars from vsphere-ws/java/JAXWS/lib  
    * download the vsan-sdk-java.zip, extract its contents and copy lib/vsanmgmt-sdk.jar  
* instruct the application’s build system to use the downloaded jars during its compilation phase  
* instruct the application’s start up phase to include the downloaded jars in the classpath

This developer setup was uncommon because it does not follow the industry standards.
The VCF 9.0 SDK addresses them and other problems discussed in the document.

# Getting started

The VCF 9.0 SDK covers the following services:

* vCenter  
* vCenter Single Sign-On Security Token Service (STS)   
* Storage Monitoring Service (SMS)  
* Policy Based Management (PBM)  
* Virtual Storage Lifecycle Management (VSLM)  
* vSAN  
* SDDC Manager  
* VMware Cloud Foundation Installer (VCF Installer)

The SDK provides bindings, utility code that supplements them and samples that demonstrate common API usage. It is distributed as a set of Maven dependencies, making it possible to plug into modern build systems such as Gradle, Maven and any other build system that’s capable of handling Maven artifacts.

The table below represents the GAV (groupId, artifactId and version) coordinates of each component:

| groupId        | artifactId          | version | Notes                                                                                             |
|----------------|---------------------|---------|---------------------------------------------------------------------------------------------------|
| com.vmware.sdk | vim25               | 9.0.0.0 | vCenter \+ vSAN WSDL bindings                                                                     |
| com.vmware.sdk | ssoclient           | 9.0.0.0 | WSDL bindings                                                                                     |
| com.vmware.sdk | sms                 | 9.0.0.0 | WSDL bindings                                                                                     |
| com.vmware.sdk | pbm                 | 9.0.0.0 | WSDL bindings                                                                                     |
| com.vmware.sdk | vslm                | 9.0.0.0 | WSDL bindings                                                                                     |
| com.vmware.sdk | eam                 | 9.0.0.0 | WSDL bindings                                                                                     |
| com.vmware.sdk | sddc-manager        | 9.0.0.0 | OpenAPI bindings                                                                                  |
| com.vmware.sdk | vcf-installer       | 9.0.0.0 | OpenAPI bindings                                                                                  |
| com.vmware.sdk | vcenter             | 9.0.0.0 | vCenter REST API bindings                                                                         |
| com.vmware.sdk | vsan-dp             | 9.0.0.0 | vSAN DP REST API bindings                                                                         |
| —              | —                   | —       | —                                                                                                 |
| com.vmware.sdk | ssoclient-utils     | 9.0.0.0 | Utility code for SAML token-related operations.                                                   |
| com.vmware.sdk | vsphere-utils       | 9.0.0.0 | Utility code for common vSphere related needs such as authentication, client initialization, etc. |
| com.vmware.sdk | vcf-installer-utils | 9.0.0.0 | Utility code for common VCF Installer related needs.                                              |

From a dependency declaration perspective, there are 2 ways to declare dependencies:  using the GAV coordinates to declare each dependency individually or by importing the VCF 9.0 SDK BOM and delegating the version management to it. Examples:

GAV:

```kotlin
implementation("com.vmware.sdk:vim25:9.0.0.0")
```
or 

```xml
<dependency>
    <groupId>com.vmware.sdk</groupId>
    <artifactId>vim25</artifactId>
    <version>9.0.0.0</version>
</dependency>
```

BOM:

```kotlin
implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.0.0.0"))
implementation("com.vmware.sdk:vim25")
```

or

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.vmware.sdk</groupId>
            <artifactId>vcf-sdk-bom</artifactId>
            <version>9.0.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.vmware.sdk</groupId>
        <artifactId>vim25</artifactId>
    </dependency>
</dependencies>
```

From application-development perspective, there are 2 ways to declare dependencies: using the \*-utils (e.g. vsphere-utils), which will pull-in the bindings, and will provide various “helpers”, whose usage is shown in the samples, or by declaring dependencies to specific bindings (e.g. vim25) and writing code on top of them. The general recommendation is to use the \*-utils.

## Key SDK concepts

The VCF 9.0 Java SDK is an evolution of the existing SDKs \- the various SDKs are unified and integrated under a single umbrella. For the most part they are compatible with previous versions, but still require some amount of adaptation in the pre-existing consuming application code.

Different components of the VCF product have different APIs \- for example, the majority of the core vSphere stack exposes WSDL-based APIs. This is applicable for vCenter, STS, vSAN, SMS, PBM, VSLM and EAM. The maven artifacts contain automatically generated bindings which are compatible with Jakarta EE 9 (Jakarta XML Binding 3.0). The SDK-provided utility code is built on top of Apache CXF 4.0.

Additionally there are components (vCenter, SDDC Manager, VCF Installer) which have REST APIs, described with OpenAPI definitions. The SDK provides bindings, as well as utility code, for them as well.

## Migrating existing applications to the new SDK

### Build system changes

The application’s build process has to be updated to one of the options below:

* In case the build system has Internet access, it can download the SDK components from Maven Central  
* For air-gapped build environments, the application developer has to download vcf-sdk-java.zip from Broadcom’s developer portal. The archive contains a top-level maven directory with the SDK-provided components. The application developer should incorporate it in the build system (e.g. by uploading the Maven artifacts to an internal mirror) and should also make sure that all direct and transitive dependencies, specified in the POM descriptor, are available at compile and run time.

The VCF 9.0 SDK supports JDK 11, 17 and 21\.

Note 1: The SDK no longer provides \*-samples.jar(s). If the application code relied on such jars, it should be adapted to replace this dependency with the newly provided \*-utils alternatives e.g. **vsphere-utils**.

Note 2: Starting with VCF 9.0, all SDK components come with PGP signatures, ensuring their authenticity. The application build system can be updated to include a verification that the artifacts are signed with the PGP key that has the following fingerprint: **1131612154DCAD7C88766B56DA1F25B6A757434F**.

### Application code changes

For the most part heavy changes that require consuming code adaptation on upgrade of the SDK (or adoption of the VCF SDK, as it is its first release here) have been attempted to be reduced to the bare minimum, but in some cases breakages were unavoidable and/or the net benefits outweigh the downsides. Such improvements include unification, better integration and cooperation of the various modules of the single VCF SDK.

#### Common client initialization

##### vCenter

The samples which demonstrate the WSDL-based APIs used to come with **com.vmware.connection.Connection** interface which had 2 implementations: **com.vmware.connection.SsoConnection** and **com.vmware.connection.BasicConnection.**

The samples which demonstrate the REST APIs used to come with **vmware.samples.common.VimAuthenticationHelper** and **vmware.samples.common.VapiAuthenticationHelper**.

Those classes were sample code used to demonstrate how to establish a session in the two different API endpoints. With the 9.0 SDK there’s a new **vsphere-utils** artifact, available on Maven Central, that can be consumed as a standalone library. It contains reusable utility code that sets up clients, authenticates them and hides away common boilerplate code. 

**com.vmware.sdk.vsphere.utils.VcenterClientFactory** is a factory which is expected to be used on a per-instance basis. It has multiple constructors, making it possible to configure various aspects of the underlying HTTP client:

* server address \- FQDN or IP address of the remote server  
* port \- the TCP port of the HTTPS endpoint, typically 443  
* verifyHostname \- whether to validate the hostname during the TLS handshake (by default \- true); for insecure connections this should be false  
* trustStore \- a user-supplied trust store in case vCenter’s certificate is not issued by a well-known CA; for insecure connections this should be an empty keystore; null, the default, means “use JVM’s default CA keystore”

Once a factory is initialized, it can be used to create **com.vmware.sdk.vsphere.utils.VcenterClient** instances which can be used to obtain stubs for the REST APIs as well as the WSDL-based ones.

```java
import com.vmware.appliance.system.Version;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.VimPortType;

VcenterClientFactory factory = new VcenterClientFactory(serverAddress);
try (VcenterClient client = factory.createClient(...)) {
    // The VimPortType is the same interface as older versions
    // The difference is that now its implementation uses Apache CXF instead of jaxws-rt
    VimPortType vimPort = client.getVimPort();

    // Alternatively, to access a REST API stub, simply call client.createStub(someClass), e.g.
    Version version = vimClient.createStub(Version.class);
}
```

VcenterClientFactory::createClient has a couple of overloads:

```java
// functionally equivalent to the old BasicConnection.
// Under the hood it uses VimPortType::login to create a vCenter session.
public VcenterClient createClient(String username, String password, String locale)
```

```java
// functionally equivalent to the old SsoConnection.
// Under the hood it fetches a SAML HoK token from the STS service
// and uses VimPortType::loginByToken to create a vCenter session.
public VcenterClient createClient(AbstractHokTokenAuthenticator auth)
```

```java
// This overload is new in the sense that there wasn’t a dedicated Connection implementation,
// but it’s not a new authentication scheme - under the hood it fetches a SAML Bearer token
// from the STS service and uses VimPortType::loginByToken to create a vCenter session i.e. uses existing APIs.
public VcenterClient createClient(BearerTokenAuthenticator auth)
```

The VcenterClient implements the **java.io.Closeable** interface. Whenever the client is no longer needed, it should be closed by calling the **close()** method \- this releases allocated resources on the server (e.g. the session will be invalidated). As a rule of thumb, it is recommended to use the try-with-resources block which will call the **close()** method at the end of the code block.

**Example \#1:**

```java
String serverAddress = "vcenter1.mycompany.com"
String username = "Administrator@vsphere.local";
String password = "password";
try (VcenterClient client = factory.createClient(username, password, null)) {
  .. // code that uses client.getVimPort() or client.createStub(..)
}
```

**Example \#2:**

```java
KeyStore userKeyStore = loadKeystore(); // e.g. read a keystore from the file system
PrivateKey privateKey = getPrivateKey(userKeyStore); // find the first private key
X509Certificate certificate = getCertificate(userKeyStore); // find the associated certificate

// This authenticator is going to log into the STS & acquire an HoK token.
AbstractHokTokenAuthenticator authenticator = new HokTokenAuthenticator(
	serverAddress,
	443,
	new SimpleHttpConfigurer(createTrustManagerFromCertificate(trustStorePath)),
	username,
	password,
	privateKey,
	certificate,
	null);

try (VcenterClient client = factory.createClient(authenticator)) {
  .. // code that uses client.getVimPort() or client.createStub(..)
}
```

**Example \#3:**

```java
String serverAddress = "vcenter1.mycompany.com"
String username = "Administrator@vsphere.local";
String password = "password";

// This authenticator is going to log into the STS & acquire a Bearer token.
BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(
	serverAddress,
	443,
	new SimpleHttpConfigurer(createTrustManagerFromCertificate(trustStorePath)),
	username,
	password,
	null);

try (VcenterClient client = factory.createClient(authenticator)) {
    .. // code that uses client.getVimPort() or client.createStub(..)
}
```

###### *STS (ssoclient)*

Prior to the 9.0 release the SDK had sample code (“ssoclient”) used to demonstrate how to acquire a SAML token and how to use it in order to log into vCenter and create a session. These use cases have been covered in the VcenterClientFactory::createClient.

If for some reason access to the SAML token is needed, the SDK provides the **ssoclient-utils** component.

**com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator** is a stateless class with a few acquireXyzToken(...) methods.

Example - acquire a Bearer token:

```java
import java.time.Duration;
import org.w3c.dom.Element;
import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

String serverAddress = "vcenter1.mycompany.com";
String username = "Administrator@vsphere.local";
String password = "password";
Duration tokenLifetime = Duration.ofMinutes(30);

TrustManager trustManager = ..; // Instantiate a TrustManager used to verify the STS server certificate.

SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(trustManager);
Element token = WsTrustAuthenticator.acquireBearerTokenForRegularUser(
	serverAddress, 443, portConfigurer, username, password, tokenLifetime);
```

Example - acquire a HoK token:

```java
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.w3c.dom.Element;
import com.vmware.sdk.ssoclient.utils.WsTrustAuthenticator;
import com.vmware.sdk.utils.wsdl.SimpleHttpConfigurer;

String serverAddress = "vcenter1.mycompany.com";
String username = "Administrator@vsphere.local";
String password = "password";
Duration tokenLifetime = Duration.ofMinutes(30);

TrustManager trustManager = ..; // Instantiate a TrustManager used to verify the STS server certificate.
X509Certificate certificate = ..; // Retrieve the certificate from existing keystore, read it from a file, or something else.
PrivateKey key = ..; // Retrieve the associated private key from existing keystore, read it from a file, or something else.

SimpleHttpConfigurer portConfigurer = new SimpleHttpConfigurer(trustManager);
Element token = WsTrustAuthenticator.acquireHokTokenWithUserCredentials(
	serverAddress, 443, portConfigurer, username, password, key, certificate, tokenLifetime);

// Alternatively, if you have an existing HoK token:
Element token2 = WsTrustAuthenticator.acquireHokTokenWithUserCredentials(
        serverAddress, 443, portConfigurer, token, key, certificate, tokenLifetime);
```

Once a valid SAML token has been obtained, it can be used to authenticate into vCenter.
```java
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.w3c.dom.Element;
import com.vmware.sdk.ssoclient.utils.soaphandlers.HeaderHandlerResolver;
import com.vmware.sdk.ssoclient.utils.soaphandlers.SamlTokenHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.TimeStampHandler;
import com.vmware.sdk.ssoclient.utils.soaphandlers.WsSecuritySignatureAssertionHandler;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

Element token = ..; // The token that was retrieved above.

// If the token is of type "HoK", these are required. They're optional for Bearer tokens.
X509Certificate certificate = ..;
PrivateKey key = ..;

// Create a VimService, prepare the signing handlers, and invoke loginByToken()
VimService vimService = new VimService();
HeaderHandlerResolver handlerResolver = new HeaderHandlerResolver();
handlerResolver.addHandler(new TimeStampHandler());
handlerResolver.addHandler(new SamlTokenHandler(token));
if (privateKey != null && certificate != null) {
    handlerResolver.addHandler(new WsSecuritySignatureAssertionHandler(
            privateKey, certificate, SoapUtils.getNodeProperty(token, "ID")));
}

vimService.setHandlerResolver(handlerResolver);

VimPortType vimPort = vimService.getVimPort();
PortConfigurer portConfigurer = ..; // PortConfigurer to setup TLS, timeouts, cookies, etc.
portConfigurer.configure((BindingProvider) vimPort, createVimUrl(serverAddress, port));

ServiceContent serviceContent = vimPort.retrieveServiceContent(getVimServiceInstanceRef());
vimPort.loginByToken(serviceContent.getSessionManager(), null);
```

###### *SMS / PBM / VSLM*
Prior to the 9.0 release the sample code was somewhat interconnected and/or there were code duplications. The new SDK solves these problems with the use of the VcenterClient. Everything discussed above \- creating VcenterClientFactory and creating a client by providing credentials is still applicable. This VcenterClient instance can be used to create a WSDL ready-to-use port for the corresponding service as shown below:

```java
String serverAddress = "vcenter1.mycompany.com"
String username = "Administrator@vsphere.local";
String password = "password";
try (VcenterClient client = factory.createClient(username, password, null)) {
    SmsPortType smsPort = client.getSmsPort();
    PbmPortType pbmPort = client.getPbmPort();
    VslmPortType vslmPort = client.getVslmPort();
}
```

Essentially this replaces:

* **com.vmware.spbm.connection.Connection**  
* **com.vmware.spbm.connection.BasicConnection**  
* **com.vmware.fcd.helpers.VslmConnection**

###### *vSAN*

Prior to the 9.0 release the vSAN SDK used to expect that the user had already downloaded & prepared the vSphere Management SDK (i.e. vim25.jar & co. had to be used in conjunction with vsanmgmt-sdk.jar). This is no longer the case because the 9.0 version of vim25.jar contains both the VIM and vSAN bindings. User should remove vsanmgmt-sdk.jar and use vSAN binding in 9.0 version of vim25.jar. Assuming that the application code already depends on **vsphere-utils**, the vSAN client initialization boils down to:

```java
String serverAddress = "vcenter1.mycompany.com"
String username = "Administrator@vsphere.local";
String password = "password";

VcenterClientFactory factory = new VcenterClientFactory(serverAddress);
try (VcenterClient client = factory.createClient(username, password, null)) {
    VsanhealthPortType vsanPort = client.getVsanPort();
}
```

Alternatively, if the user application wishes to talk to the vSAN’s ESXi API endpoint:

```java
String serverAddress = "esx1.mycompany.com"
String username = "root";
String password = "password";

ESXiClientFactory factory = new ESXiClientFactory(serverAddress);
try (ESXIClient client = factory.createClient(username, password, null)) {
    VsanhealthPortType vsanPort = client.getVsanPort();
}
```

Some of the data objects for request/responses have been moved to the **com.vmware.vsan.sdk** package \- e.g. **com.vmware.vim25**.VsanClusterHealthSummary.

Some duplicate classes from the **com.vmware.vsan.sdk** package have been deleted and they’re supposed to be used from the com.vmware.vim25 package.

These duplicate data objects were needed because the operations were duplicated between the **VsanHealthPortType** and **VimPortType**. These things were cleaned up and now:

* VsanhealthVimPortType has been deleted. The application code should use the appropriate APIs through the standard **VimPortType**.
* VsanhealthPortType contains only vsan-related operations. Where necessary, the data objects are shared between the bindings for vsan & vim25.

com.vmware.vsan.util.VsanUtil has been moved to **com.vmware.sdk.vsphere.utils.vsan.management.VsanUtil** (part of **vsphere-utils**) and it has been updated to use the **PropertyCollectorHelper**.

com.vmware.vsan.connection.SpbmConnection, com.vmware.vsan.connection.VimConnection, and com.vmware.vsan.connection.VsanHealthConnection are removed, use com.vmware.sdk.vsphere.utils.VcenterClientFactory or com.vmware.sdk.vsphere.utils.ESXiClientFactory to access vSAN APIs on vCenter or ESXi hosts respectively.

The `build.py` script provided in pre 9.0 vSAN SDK for re-generating vSAN binding and building vSAN samples does not work anymore because of the 9.0 VCF SDK change. User shall check [vcf-api-specs](https://github.com/vmware/vcf-api-specs) repo for regenerating binding and [vcf-sdk-java](https://github.com/vmware/vcf-sdk-java) repository for building and running vSAN java samples.

##### Advanced client initialization

The VcenterClientFactory exposes configuration properties (via constructor overloads) trying to handle the most common use cases. If they aren’t sufficient for a particular scenario, the following constructor can be used:

```java
public VcenterClientFactory(
    String serverAddress, int port, PortConfigurer portConfigurer, HttpConfiguration vApiHttpConfiguration)
```

The PortConfigurer is an interface whose job is to configure Jakarta port bindings (e.g. instances of VimPortType, VsanhealthPortType, etc.)

For example, to use a proxy, implement the interface (or extend the SimpleHttpConfigurer) and override the following method:

```java
void configure(BindingProvider provider, URI url);
```

Example implementation:

```java
import org.apache.cxf.endpoint.Client;

Client client = ClientProxy.getClient(provider);
HTTPConduit http = (HTTPConduit) client.getConduit();
http.getClient().setProxyServer("proxy1.mycompany.com");
http.getClient().setProxyServerPort(8080);
```

The `HttpConfiguration` is used whenever the client uses the REST API stubs (i.e. uses client.creaStub(SomeClass.class)). To create HTTP settings with a proxy:

```java
HttpConfiguration.Builder httpConfigBuilder = new HttpConfiguration.Builder(); // alternatively use com.vmware.sdk.utils.ssl.vapi.HttpConfigHelper.createDefaultHttpConfiguration(..)

HttpConfiguration.ProxyConfiguration proxyConfiguration =
            	HttpConfiguration.ProxyConfiguration.custom("proxy1.mycompany.com", 8080).getConfig();
httpConfigBuilder.setProxyConfiguration(proxyConfiguration);

HttpConfiguration cfg = httpConfigBuilder.getConfig();
```

##### Using custom trust store

In some cases the remote server certificate might be issued by a CA that’s not “well-known”. To handle this case, the application can use the following constructor:

```java
public VcenterClientFactory(String serverAddress, KeyStore trustStore)
```

Where the provided “trustStore” contains the appropriate root certificate(s). 

##### Insecure clients

By default the SDK follows a “secure-by-default” approach which forces application developers to explicitly disable security checks when talking to remote services. The most common ones are related to the remote certificate verification and hostname verification when establishing a TLS connection.  
In certain situations, e.g. during development or testing, it may be easier to disable those verifications, which can be achieved by using the following constructor:

```java
public VcenterClientFactory(String serverAddress, boolean verifyHostname, KeyStore trustStore)
```

The “verifyHostname” argument should be set to false and the provided “trustStore” must be non-null and without any keys in it (which indicates that there are no trusted root CA certificates). However, special care must be taken turning off security knobs \- if the application code goes into production with improper configuration, it is going to be susceptible to man-in-the-middle attacks\!

##### Migrating  business logic to the new utility code

The following classes have been deleted:

**com.vmware.samples.automation.common.vim.helpers.VimUtil**   
**com.vmware.samples.automation.common.vim.helpers.WaitForValues**  
**com.vmware.samples.vcenter.management.connection.helpers.GetMOREF**  
**com.vmware.samples.vcenter.management.connection.helpers.WaitForValues**

The alternative, part of **vsphere-utils**, is **com.vmware.sdk.vsphere.utils.PropertyCollectorHelper.**

To instantiate a PropertyCollectorHelper, simply create a VcenterClient and get a VimPortType reference:

```java
String serverAddress = "vcenter1.mycompany.com"
String username = "Administrator@vsphere.local";
String password = "password";
try (VcenterClient client = factory.createClient(username, password, null)) {
    VimPortType vimPort = client.getVimPort();
    PropertyCollectorHelper pcHelper = new PropertyCollectorHelper(vimPort);
}
```

Example \- await for task completion:

```java
ManagedObjectReference vmMoRef = …; // reference to a vm
ManagedObjectReference powerOnTask = vimPort.powerOnVMTask(vmMoRef, null);
pcHelper.awaitTaskCompletion(powerOnTask); // wait for the task to finish
```

Example \- retrieve a specific property of a managed object:

```java
ManagedObjectReference vmMoRef = …; // reference to a vm
VirtualHardware hw = propertyCollectorHelper.fetch(vmMoRef, "config.hardware");
```

