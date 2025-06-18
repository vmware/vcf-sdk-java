This directory contains below samples for inventory APIs:

1. Bulk Transition

   | Sample                       | Description                                                                 |
   |------------------------------|-----------------------------------------------------------------------------|
   | BulkExtract.java             | Demonstrates extracting clusters/standalone hosts in bulk                   |
   | BulkExtractTransition.java   | Demonstrates extracting and transitioning clusters/standalone hosts in bulk |
   | BulkTransition.java          | Demonstrates transitioning clusters/standalone hosts in bulk                |

### Running the samples:

    #For example, you can use the following command to run transition on a group of clusters
    ./gradlew :vsphere-samples:run -Pexample=com.vmware.sdk.samples.vcenter.management.vlcm.inventory.bulktransition.BulkExtractTransition --args='--serverAddress "<vcenter_ip>" --username "<user>" --password "<password>" --entityType "CLUSTER" --entities "domain-c11,domain-c13"' -I ../../init-dev.gradle.kts build

### Testbed Requirement:

    - vCenter Server >= 9.0.0+
    - A datacenter
    - If a cluster is used as an entity, cluster should have at least one host.
    - Host with version >= 7.0.2
