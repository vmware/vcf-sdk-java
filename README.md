# VCF SDK for Java

1. [Overview](#overview)
2. [SDK Compatibility](#sdk-compatibility)
3. [Using the SDK in custom applications](#using-the-sdk-in-custom-applications)
   1. [Getting started](#getting-started) 
   2. [Fine-grain control over the dependency management](#fine-grain-control-over-the-dependency-management)
   3. [Migrating existing applications to consume the VCF 9.0 Java SDK](#migrating-existing-applications-to-consume-the-vcf-90-java-sdk)
4. [How to run the samples](#how-to-run-the-samples)
   1. [How to run the vCenter samples](#how-to-run-the-vcenter-samples)
   2. [How to run the vSAN samples](#how-to-run-the-vsan-samples)
   3. [How to run the SDDC Manager samples](#how-to-run-the-sddc-manager-samples)
   4. [How to run the VCF Installer samples](#how-to-run-the-vcf-installer-samples)
5. [Logging configuration](#logging-configuration)
6. [IDE Support](#ide-support)
7. [API Documentation](#api-documentation)
8. [SDK Support](#sdk-support)

## Overview

This repository holds the Java-based VCF 9.0 SDK samples and utilities. Structure:

```
[/{module}-samples]
/utils
    [/{module}-utils]
```

`/{module}` - contains all samples for a VCF component like vSphere, SDDC Manager, etc.

`[/{module}-utils]` - contains reference implementation of common API usages.
Large components may be split into multiple modules e.g. vCenter has sms, spbm, vslm etc.

The project is built on-top of [Gradle](https://gradle.org/) 8 and uses Multi-Project structure.
`buildSrc` contains build scripts shared between all subproject.

`buildSrc/src/main/kotlin/java-conventions.gradle.kts` defines a Gradle convention plugin which controls the Maven
repositories used to fetch the necessary dependencies. The options are:

- Maven Central - This is the easiest and most commonly used option. It contains VMware's first-party dependencies,
as well as third party dependencies. Requires Internet access.
- A `maven` directory placed inside the root of the project - by default it is not part of the Git repository,
but this is useful for air-gapped environments where dependencies can't be fetched from the Maven Central.
In such cases the code has been downloaded from Broadcom's developer portal ([vcf-java-sdk.zip](https://developer.broadcom.com/sdks/vcf-java-sdk/latest)).
Note that this is going to provide only the first-party dependencies (e.g. vim25), but not third-party ones (e.g. jackson).
Such type of use case might require adding an extra maven repository pointing to a self-hosted server, e.g:
```kotlin
maven {
  url = uri("https://internal-repo.my-company.com/maven")
}
```

To build the samples and the utility code execute the following command from the root folder:
```shell
./gradlew build
```

After making code changes, the build might fail because the tasks `spotlessJavaCheck` or `spotlessKotlinGradleCheck`
detect code style issues. To apply the necessary format rules (line endings, ordering of imports, code conventions, etc.):
````shell
./gradlew spotlessApply
````

## SDK Compatibility

### Java compatibility

The SDK is compatible with the following Java LTS versions: 11, 17, 21.
It is **_strongly_** recommended to use one of those versions when integrating the SDK into custom applications and when running the samples.

### VCF component compatibility

The SDK is compatible with the following components:

1. VMware vCenter 8.0 and 9.0
2. VMware vSAN 8.0 and 9.0
3. VMware Cloud Foundation 9.0
   1. SDDC Manager 9.0
   2. VCF Installer 9.0

## Using the SDK in custom applications

### Getting started

The quickest way to declare SDK dependency into custom application is to import VCF SDK BOM and the utility projects demonstrated in the samples:

Gradle:
```kotlin
implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.0.0.0"))
implementation("com.vmware.sdk:vsphere-utils")
implementation("com.vmware.sdk:vcf-installer-utils")
```

or

Maven:
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
        <artifactId>vsphere-utils</artifactId>
    </dependency>

    <dependency>
       <groupId>com.vmware.sdk</groupId>
       <artifactId>vcf-installer-utils</artifactId>
    </dependency>
</dependencies>
```

After that simply start using the new dependencies, e.g.:

```java
import static com.vmware.sdk.vsphere.utils.VimClient.getVimServiceInstanceRef;

import java.text.SimpleDateFormat;

import javax.xml.datatype.XMLGregorianCalendar;

import com.vmware.appliance.system.Version;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vim25.VimPortType;

public class Application {
    public static void main(String[] args) {
        // Creates a secure connection by default.
        // To disable the TLS verifications, pass an empty non-null trust store in the constructor.
        VcenterClientFactory factory = new VcenterClientFactory("vc1.mycompany.com");

        try (VcenterClient client = factory.createClient(username, password, null)) {
            VimPortType vimPort = client.getVimPort();
            XMLGregorianCalendar time = vimPort.currentTime(getVimServiceInstanceRef());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss.SSSZ");
            System.out.println(
                "Server current time: " +
                sdf.format(time.toGregorianCalendar().getTime()));

            Version version = client.createStub(Version.class);
            System.out.println("Server version: " + version.get().getVersion());
        }
    }
}

```

### Fine-grain control over the dependency management

From application-development perspective, there are 2 ways to declare dependencies: using the \*-utils (e.g. vsphere-utils), which will pull-in the bindings, and will provide various “helpers”, whose usage is shown in the samples, or by declaring dependencies to specific bindings (e.g. vim25) and writing code on top of them. The general recommendation is to use the \*-utils.

From a dependency declaration perspective, there are 2 ways to declare dependencies: by importing the VCF 9.0 SDK BOM and delegating the version management to it or by using the GAV coordinates to declare each dependency individually. Examples:

#### BOM

```kotlin
implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.0.0.0"))
implementation("com.vmware.sdk:vsphere-utils")
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
        <artifactId>vsphere-utils</artifactId>
    </dependency>
</dependencies>
```

The full list of components in the BOM can be found in the `sdkLibs` catalog of [settings.gradle.kts](settings.gradle.kts).

#### GAV

Gradle:
```kotlin
implementation("com.vmware.sdk:vsphere-utils:9.0.0.0")
```
or

Maven:
```xml
<dependency>
    <groupId>com.vmware.sdk</groupId>
    <artifactId>vsphere-utils</artifactId>
    <version>9.0.0.0</version>
</dependency>
```

### Migrating existing applications to consume the VCF 9.0 Java SDK

Please follow the [migration-guide.md](migration-guide.md) for detailed step-by-step migration guide.

## How to run the samples

There are 2 ways to run samples:

- import the project in an IDE and use its "Run" capability in order to execute the main(String[] args) method of the sample
- using gradle commands through the terminal

The basic syntax is this:
```shell
./gradlew :<module>-samples:run -Pexample=fully.qualified.ClassName --args='--arg-name-1 arg-value-1 --arg-name-2 arg-value-2'
```

### How to run the vCenter samples

```shell
./gradlew :vsphere-samples:run -Pexample=com.vmware.sdk.samples.vcenter.monitoring.performance.PrintCounters --args='--serverAddress vc1.mycompany.com --username Administrator@vsphere.local --password vmware --entitytype VirtualMachine --entityname centos-vm --filename /tmp/counters'
```

### How to run the vSAN samples

Example:
```shell
 ./gradlew :vsphere-samples:run -Pexample=com.vmware.sdk.samples.vsan.management.VsanVcApiSample --args='--serverAddress vc1.mycompany.com --username Administrator@vsphere.local --password vmware --clusterName Vsan2Cluster'
```

### How to run the SDDC Manager samples

```shell
./gradlew :sddc-manager-samples:run -Pexample=com.vmware.sdk.samples.sddcm.domains.DeleteDomainExample --args='--sddcManagerHostname sddcm.mycompany.com --domainName domain1 --username Administrator@vsphere.local --password vmware'
```

### How to run the VCF Installer samples

```shell
./gradlew :vcf-installer-samples:run -Pexample=com.vmware.sdk.samples.vcf.installer.system.GetApplianceInfo --args='--vcfInstallerServerAddress vcf-installer.mycompany.com --vcfInstallerAdminPassword vmware'
```

## Logging configuration

The SDK comes with different flavours of logging.

All vapi-* dependencies, as well as projects under utils/*, use slf4j-api.

vim25, pbm, sms, ssoclient and vslm depend on `jaxws-rt` which uses `java.util.logging`.

The samples are configured to use [logback](https://logback.qos.ch/) logger.
samples-infrastructure/src/main/resources has 2 important files:

- logging.properties - configuration file which is read by
  `com.vmware.sdk.samples.utils.SampleCommandLineParser`; adds logback support for java.util.logging
- logback.xml - a configuration that provides somewhat sane getting started defaults, including java.util.logging
  configuration
  
## IDE support

This repository contains Java projects with multi-project Gradle build and can be used with any IDE which supports these technologies.

Some specifics about popular Java IDEs are discussed in the following subsections.

### IntelliJ IDEA

Assuming that the IDE has Java/Kotlin/Gradle plugins installed, import the project by pointing to the root or by opening settings.gradle.kts.

### Visual Studio Code

Assuming that the IDE has Java/Kotlin/Gradle plugins installed, import the project by pointing to the root folder.

### Eclipse

There are at least two ways to load the projects in Eclipse as explained below.

#### Import as Gradle Projects using Buildship plug-in

1. Make sure Buildship plug-in for Eclipse is installed.
2. Import the root folder of the repository as Existing Gradle Project.

This will discover the Gradle multi-project structure and create Eclipse projects for it.

#### Generate Eclipse project files using Gradle 'eclipse' plug-in

```shell
./gradlew build test

./gradlew cleanEclipse eclipse [-Peclipse.classpath.vars]
```

The first command above ensures all dependencies are downloaded in the Gradle build cache.

The second generates .classpath and .project files. 

Once these are available the root folder of the repository can be imported as Existing Project in Eclipse.

If `-Peclipse.classpath.vars` is used the .classpath files will use paths relative to `VCF_SDK_GRADLE_USER_HOME`. The latter must be declared as Eclipse Classpath Variable pointing to the Gradle user home, which contains the cache with dependencies.
This might be useful if the source and build trees are remote and Eclipse is accessing them 
as a mount or network share.

## API Documentation
### VCF
* [SDDC Manager](https://developer.broadcom.com/xapis/sddc-manager-api/latest/)
* [VCF Installer](https://developer.broadcom.com/xapis/vcf-installer-api/latest/)
* [VMware vSphere REST API Reference documentation](https://developer.broadcom.com/xapis/vsphere-automation-api/latest/)
* [vSphere Web Services API](https://developer.broadcom.com/xapis/vsphere-web-services-api/latest/)
* [vSAN](https://developer.broadcom.com/xapis/vsan-management-api/latest/)

## SDK Support

Support details can be referenced under the **SDK and API Support for Commercial and Enterprise Organizations** section at [Broadcom Developer Portal](https://developer.broadcom.com/support).

For community support, please open a [Github issue](https://github.com/vmware/vcf-sdk-java/issues) or start a [Discussion](https://github.com/vmware/vcf-sdk-java/discussions).


