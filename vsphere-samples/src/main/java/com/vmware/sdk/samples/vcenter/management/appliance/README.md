# Appliance Management API Samples

This directory contains samples for Appliance Management APIs:

### Appliance Health APIs

| Sample                                                  | Description                                                |
|---------------------------------------------------------|------------------------------------------------------------|
| com.vmware.samples.appliance.health.HealthMessages.java | Get the health messages for various appliance health items |

### Appliance Local Accounts APIs

| Sample                                                                          | Description                                   |
|---------------------------------------------------------------------------------|-----------------------------------------------|
| com.vmware.samples.appliance.localaccounts.globalpolicy.GlobalPolicySample.java | Demonstrates set and get Global policy values |
| com.vmware.samples.appliance.localaccounts.LocalAccountWorkflow.java            | Demonstrates local accounts workflow          |

### Appliance Networking APIs

| Sample                                                                                            | Description                                                             |
|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.appliance.networking.NetworkingWorkflow.java            | Enable/Disable IPv6, Reset and Get the network information              |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.dns.DnsDomainWorkflow.java         | Add/Set/List the DNS domains for the appliance                          |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.dns.DnsServersWorkflow.java        | Add/Set/List the DNS servers for the appliance                          |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.dns.HostNameWorkflow.java          | Get/Set the hostname for the appliance                                  |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.interfaces.InterfacesWorkflow.java | List/Get the interfaces information for the appliance                   |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.interfaces.IPv4Workflow.java       | Set/Get the IPv4 configuration of a specific interface in the appliance |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.interfaces.IPv6Workflow.java       | Set/Get the IPv6 configuration of a specific interface in the appliance |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.proxy.ProxyWorkflow.java           | List/Set/Get the proxy information for the appliance                    |
| com.vmware.sdk.samples.vcenter.management.appliance.networking.proxy.NoProxyWorkflow.java         | Get/Set the servers with No proxy configuration in the appliance        |

### TimeZone APIs

| Sample                                                    | Description                                                                                        |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| com.vmware.samples.appliance.timezone.TimeZoneSample.java | Demonstrates setting and getting TimeZone. Accepted values are valid Timezone values for appliance |

### Testbed Requirement:

    - 1 vCenter Server

### Services List operations

| Sample                                                                              | Description                                                 |
|-------------------------------------------------------------------------------------|-------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.appliance.services.list.ListServices.java | Demonstrates how to get list of Services present in vCenter |
| com.vmware.sdk.samples.vcenter.management.appliance.services.ServicesWorkflow.java  | Demonstrates services api workflow                          |

### Testbed Requirement:

    - 1 vCenter Server
    - 2 ESX hosts
    - 1 datastore
    - Some samples need additional configuration like a cluster, vm folder, standard portgroup, iso file on a datastore and distributed portgroup