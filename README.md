# VCF SDK for Java

1. [Overview](#overview)
2. [SDK Compatibility](#sdk-compatibility)
3. [Using the SDK in custom applications](#using-the-sdk-in-custom-applications)
   1. [Getting started](#getting-started) 
   2. [Fine-grain control over the dependency management](#fine-grain-control-over-the-dependency-management)
   3. [Migrating existing applications to consume VCF Java SDK 9.0 or later](#migrating-existing-applications-to-consume-vcf-java-sdk-90-or-later)
4. [How to run the samples](#how-to-run-the-samples)
   1. [How to run the vCenter samples](#how-to-run-the-vcenter-samples)
   2. [How to run the vSAN samples](#how-to-run-the-vsan-samples)
   3. [How to run the SDDC Manager samples](#how-to-run-the-sddc-manager-samples)
   4. [How to run the VCF Installer samples](#how-to-run-the-vcf-installer-samples)
   5. [How to run the VCF Operations samples](#how-to-run-the-vcf-operations-samples)
      1. [Operations samples](#operations-samples)
      2. [Log Management samples](#log-management-samples)
      3. [Operations for Networks samples](#operations-for-networks-samples)
5. [Logging configuration](#logging-configuration)
6. [IDE Support](#ide-support)

## Overview

This repository holds the Java-based VCF 9.1 SDK samples. Structure:

```
[/{module}-samples]
```

`/{module}-samples` - contains all samples for a VCF component like vSphere, SDDC Manager, etc. There is also a "samples-runner" module intended for executing the samples from Maven command line and a "samples-infrastructure" module which is used for shared helper code.

The project is built on-top of [Maven](https://maven.apache.org/) 3.9 and uses Multi-Project structure.

 - By default the build is searching for its dependencies on Maven Central where jars are published or your enterprise package repository.
 - Alternatively, the Maven build can be configured to search for its dependencies from the local machine.
    - First you should download and unzip the SDK dependencies from ([Broadcom's developer portal](https://developer.broadcom.com/sdks/vcf-java-sdk/latest/)).
       - The SDK zip file follows the naming convention `vcf-sdk-java-<VERSION>-<BUILD_NUMBER>.zip` where `VERSION` is the released version of the VCF Java SDK and `BUILD_NUMBER` is the build number of the released VCF Java SDK e.g. `vcf-sdk-java-9.1.0.0-24798170.zip`.
       - You can unzip the dependencies in any folder you wish. For the purposes in this README, the folder with unzipped dependencies will be called just `vcf-sdk-java`.
    - Note that this is going to provide only the Broadcom owned dependencies (e.g. vim25), but not third-party ones (e.g. jackson).
    - Such type of use case might require adding an extra maven repository pointing to a self-hosted server.
    - You can add such a repository in the `settings.xml` file, for example:
      ```xml
      <repository>
          <id>internal-repo</id>
          <url>https://internal-repo.my-company.com/maven</url>
      </repository>
      ```
         - You also need to add the enterprise package repository or other user-specific Maven settings you might have as a plugin repository. To do this, place something like this just below the `</repositories>` tag:
            ```xml
            <pluginRepositories>
                <pluginRepository>
                    <id>internal-plugin-repo</id>
                    <url>https://internal-repo.my-company.com/maven</url>
                </pluginRepository>
            </pluginRepositories>
            ```
         - Then execute the following command from the samples root folder which configures the Maven build to search for dependencies from a local folder:
            ```shell
            ./mvnw install -s settings.xml -Dsdk.repo=vcf-sdk-java
            ```
   - Alternatively you can transfer the setting(s) from the provided `settings.xml` to your active one.


## SDK Compatibility

### Java compatibility

The SDK is compatible with the following Java LTS versions: 11, 17, 21 and 25.
It is **_strongly_** recommended to use one of those versions when integrating the SDK into custom applications and when running the samples.

### VCF component compatibility

The SDK is compatible with the following components:

1. vSphere 8.0, 9.0 and 9.1
2. NSX 9.1
3. SDDC Manager 9.0 and 9.1
4. VCF Installer 9.0 and 9.1
5. VSAN Data protection 9.1
6. VCF Fleet lifecycle 9.1
7. VCF SDDC lifecycle 9.1
8. VCF Operations 9.1
9. VCF Operations for networks 9.1
10. VCF Log Management 9.1
11. VCF Real-time metrics 9.1

## Using the SDK in custom applications

### Getting started

The quickest way to declare SDK dependency into custom application is to import VCF SDK BOM and the utility libraries:

Gradle:
```kotlin
implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.1.0.0"))
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
            <version>9.1.0.0</version>
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

From a dependency declaration perspective, there are 2 ways to declare dependencies: by importing the VCF 9.1 SDK BOM and delegating the version management to it or by using the GAV coordinates to declare each dependency individually. Examples:

#### BOM

```kotlin
implementation(platform("com.vmware.sdk:vcf-sdk-bom:9.1.0.0"))
implementation("com.vmware.sdk:vsphere-utils")
```

or

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.vmware.sdk</groupId>
            <artifactId>vcf-sdk-bom</artifactId>
            <version>9.1.0.0</version>
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

The full list of components in the BOM can be found in its `pom.xml` file - `vcf-sdk-java/com/vmware/sdk/vcf-sdk-bom/9.1.0.0/vcf-sdk-bom-9.1.0.0.pom`.

#### GAV

Gradle:
```kotlin
implementation("com.vmware.sdk:vsphere-utils:9.1.0.0")
```
or

Maven:
```xml
<dependency>
    <groupId>com.vmware.sdk</groupId>
    <artifactId>vsphere-utils</artifactId>
    <version>9.1.0.0</version>
</dependency>
```

### Migrating existing applications to consume VCF Java SDK 9.0 or later

Please follow the [migration-guide.md](migration-guide.md) for detailed step-by-step migration guide.

## How to run the samples

There are 2 ways to run samples:

- import the project in an IDE and use its "Run" capability in order to execute the main(String[] args) method of the sample
- using Maven commands through the terminal

The basic syntax is this:
```shell
./mvnw compile exec:java -Dexec.mainClass="fully.qualified.ClassName" -Dexec.args='--arg-name-1 arg-value-1 --arg-name-2 arg-value-2'
```

To run the samples using dependencies from the local machine or an air-gapped environment, you would have to override the Maven repository with the `settings.xml` file:
```shell
./mvnw compile exec:java -s settings.xml -Dsdk.repo=vcf-sdk-java -Dexec.mainClass="fully.qualified.ClassName" -Dexec.args='--arg-name-1 arg-value-1 --arg-name-2 arg-value-2'
```

### How to run the vCenter samples

```shell
./mvnw compile exec:java -Dexec.mainClass="com.vmware.sdk.samples.vcenter.monitoring.performance.PrintCounters" -Dexec.args='--serverAddress vc1.mycompany.com --username Administrator@vsphere.local --password vmware --entitytype VirtualMachine --entityname centos-vm --filename /tmp/counters'
```

### How to run the vSAN samples

```shell
./mvnw compile exec:java -Dexec.mainClass="com.vmware.sdk.samples.vsan.management.VsanVcApiSample" -Dexec.args='--serverAddress vc1.mycompany.com --username Administrator@vsphere.local --password vmware --clusterName Vsan2Cluster'
```

### How to run the SDDC Manager samples

```shell
./mvnw compile exec:java -Dexec.mainClass="com.vmware.sdk.samples.sddcm.domains.DeleteDomainExample" -Dexec.args='--sddcManagerHostname sddcm.mycompany.com --domainName domain1 --username Administrator@vsphere.local --password vmware'
```

### How to run the VCF Installer samples

```shell
./mvnw compile exec:java -Dexec.mainClass="com.vmware.sdk.samples.vcf.installer.system.GetApplianceInfo" -Dexec.args='--vcfInstallerServerAddress vcf-installer.mycompany.com --vcfInstallerAdminPassword vmware'
```

### How to run the VCF Operations samples

#### Operations samples

__Note__: The Operations samples accept their arguments from configuration files. Update client-config.json along with configuration file specific to the sample. Ex: update symptom-and-alert-definition-config.json for SymptomsAndAlertDefinitions.java sample.
```shell
./mvnw exec:java -Dexec.mainClass="com.vmware.sdk.samples.ops.symptomdefinitions.SymptomsAndAlertDefinitions" -Dexec.args='src/main/resources/config/client-config.json src/main/resources/config/symptom-and-alert-definition-config.json'
```

#### Log Management samples

```shell
./mvnw exec:java -Dexec.mainClass="com.vmware.sdk.samples.ops.logs.BasicSearchExample" -Dexec.args='--username vmware --password vmware --logsHost logs.mycompany.com --logsPort 9543 --opsHost ops.mycompany.com'
```

#### Operations for Networks samples

```shell
./mvnw exec:java -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.info.Version" -Dexec.args='--username vmware --password vmware --hostName host.mycompany.com'
```

## Logging configuration

All vapi-* dependencies use slf4j-api.

vim25, pbm, sms, ssoclient and vslm depend on `cxf` which uses slf4j as well.

The samples code is also using slf4j for logging and are configuring [Logback](https://logback.qos.ch/) as the logging implementation. This demonstrats how an application can direct its own log statements together with the log statements originating in the VCF SDK modules to the same logging subsytem.

samples-infrastructure/src/main/resources has logback.xml - a configuration that provides somewhat sane getting started defaults

## IDE support

This repository contains Java projects with multi-project Maven build and can be used with any IDE which supports these technologies.

Some specifics about popular Java IDEs are discussed in the following subsections.

### Common for all IDEs in air-gapped environment

You need to copy the `settings.xml` file to another one for IDE purposes, for example `ide_settings.xml`. Then replace `${sdk.repo}` in the `ide_settings.xml` file with the path to the Maven SDK libraries folder.

### IntelliJ IDEA

If you have to use dependencies from the local machine or different repository than Maven Central, make sure you override the user settings file from File -> Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> on "User settings file" click "Override" and set the path to the custom `ide_settings.xml` file.

Import the project by pointing to the root.

### Visual Studio Code

If you have to use dependencies from the local machine or different repository than Maven Central, make sure you also configure your Maven VSCode extension to search for dependencies from the local SDK Maven libraries directory by using the custom `ide_settings.xml` file, for example using the `java.configuration.maven.userSettings` option.

Assuming that the IDE has Java and Maven plugins installed, import the project by pointing to the root folder.

### Eclipse

If you have to use dependencies from the local machine or different repository than Maven Central, before importing the project into your workspace, click on Window -> Preferences -> Maven -> User Settings -> on "User Settings" set it to the path to the custom `ide_settings.xml` file. 

Import the samples by pointing to the root and importing all subprojects as existing Maven projects.
