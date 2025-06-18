Date: February 27, 2025

This directory contains samples for managing vCenter HA Clusters.

The samples were tested against vSphere 9.0.0.0

### vCenter HA Cluster List Operations

| Sample                                                       | Description                                                                                              |
|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.ha.VchaClient.java | Demonstrates listing active node information, vCenter HA cluster information and vCenter HA cluster mode |

### vCenter HA Cluster Deploy/Undeploy Operations

| Sample                                                           | Description                                                                                                                                                                                                                       |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.ha.VchaClusterOps.java | Demonstrates vCenter HA Cluster Deploy, Undeploy Operations for a given vCenter server with automatic cluster configuration and IPv4 network configuration. The sample requires IPv4 network configuration for cluster networking |

### Testbed Requirement:

    - 3 ESXi hosts on version 8.0x or later is recommended
    - 1 Management vCenter Server on version 8x or later is recommended (Optional)
    - 1 vCenter Server Appliance on version 9.0.0.0
    - Separate network for vCenter HA than the management network (network latency between the vCenter HA cluster nodes must be less than 10ms)
    - Refer to the VCHA documentation for a full list of software and hardware requirements
