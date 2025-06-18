rootProject.name = "vcf-java-sdk"

include("samples-infrastructure")
include("sddc-manager-samples")
include("vcf-installer-samples")
include("vsphere-samples")

listOf("vmware-sdk-common", "wsdl-utils", "ssoclient-utils", "vsphere-utils", "vapi-samltoken", "vapi-authentication",
        "vcf-installer-utils").forEach {
    module ->
        include("utils:$module")
}

dependencyResolutionManagement {
    versionCatalogs {
        // Everything under `libs` is only used in the examples.
        create("libs") {
            version("slf4j", "2.0.16")

            // management sdk dependencies
            library("jakarta-ws-api", "jakarta.xml.ws:jakarta.xml.ws-api:3.0.1")
            library("cxf-jaxws", "org.apache.cxf:cxf-rt-frontend-jaxws:4.0.6")
            library("cxf-core", "org.apache.cxf:cxf-core:4.0.6")
            library("cxf-http", "org.apache.cxf:cxf-rt-transports-http:4.0.6")
            library("cxf-logging", "org.apache.cxf:cxf-rt-features-logging:4.0.6")
            library("saaj", "com.sun.xml.messaging.saaj:saaj-impl:3.0.4")

            library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("slf4j-jul", "org.slf4j", "jul-to-slf4j").versionRef("slf4j")
            library("slf4j-jcl", "org.slf4j", "jcl-over-slf4j").versionRef("slf4j")
            library("logback-classic", "ch.qos.logback:logback-classic:1.5.16")

            // Use safe Angus version overriding the CXF dependency. See CVE-2021-44549.
            // Remove this when CXF is updated. See also vapi-samltoken and vapi-authentication projects
            library("angus-mail", "org.eclipse.angus:angus-mail:1.1.0")
        }

        // Everything under `sdkLibs` is included in the SDK BOM.
        // These artifacts are published to Maven Central for public consumption.
        create("sdkLibs") {
            version("sdk", "9.0.0.0")
            version("vapi", "2.61.2")

            // wsdl-based bindings
            library("vim25", "com.vmware.sdk", "vim25").versionRef("sdk")
            library("ssoclient", "com.vmware.sdk", "ssoclient").versionRef("sdk")
            library("sms", "com.vmware.sdk", "sms").versionRef("sdk")
            library("pbm", "com.vmware.sdk","pbm").versionRef("sdk")
            library("vslm", "com.vmware.sdk", "vslm").versionRef("sdk")
            library("eam", "com.vmware.sdk", "eam").versionRef("sdk")

            // vapi-based bindings
            library("vapi-runtime", "com.vmware.vapi", "vapi-runtime").versionRef("vapi")
            library("sddc-manager", "com.vmware.sdk", "sddc-manager").versionRef("sdk")
            library("vcf-installer", "com.vmware.sdk", "vcf-installer").versionRef("sdk")
            library("vcenter", "com.vmware.sdk", "vcenter").versionRef("sdk")
            library("vsan-dp", "com.vmware.sdk", "vsan-data-protection").versionRef("sdk")
        }

        // Dependencies for unit tests.
        create("testLibs") {
            library("junit", "org.junit.jupiter:junit-jupiter:5.10.3")
            library("easymock", "org.easymock:easymock:5.3.0")
        }
    }
}
