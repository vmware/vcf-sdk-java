# VCF Operations for networks - Entities Samples

This directory contains Java samples demonstrating how to interact with various entity types in VCF Operations for networks using the VCF SDK.

## Overview

These samples show how to perform common operations on infrastructure entities such as:
- **Hosts** - List and get ESXi hosts
- **Datastores** - List and get datastores
- **Clusters** - List and get compute clusters
- **VMs** - List and get virtual machines
- **VcDatacenters** - List and get vCenter datacenters

All samples follow a consistent pattern based on the SDK's entity management APIs.

## Prerequisites

Before running these samples, ensure you have:

1. **VCF Operations for networks** instance running and accessible
2. **Valid credentials** (username/password) with appropriate permissions
3. **Network connectivity** to the VCF Operations for networks host
4. **Java 11 or later** installed
5. **Maven** for building and running the samples

## Common Parameters

All entity samples support the following command-line parameters:

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `--hostName` | Yes | "hostName" | VCF Operations for networks host address or FQDN |
| `--username` | Yes | null | Username for authentication (required if token not provided) |
| `--password` | Yes | null | Password for authentication (required if token not provided) |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |
| `--operation` | No | "list" | Operation to perform: `list` or `get` |
| `--entityId` | No | null | Entity ID for `get` operation |

## Building the Samples

From the samples root directory:

```bash
mvn clean install
```

Or to build only the operations-networks-samples module:

```bash
mvn clean install -pl vcf-operations-samples/operations-networks-samples -am
```

## Running the Samples

### General Syntax

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.<SampleClass>" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> [additional-args]"
```

**Note**: The `--operation` parameter is optional and defaults to `"list"` if not specified.

### Hosts Sample

**List all hosts (operation parameter is optional, defaults to "list"):**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Hosts" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

**Get a specific host:**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Hosts" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --entityId <entity-id>"
```

### Datastores Sample

**List all datastores (operation parameter is optional, defaults to "list"):**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Datastores" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

**Get a specific datastore:**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Datastores" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --entityId <entity-id>"
```

### Clusters Sample

**List all clusters (operation parameter is optional, defaults to "list"):**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Clusters" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

**Get a specific cluster:**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Clusters" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --entityId <entity-id>"
```

### VMs Sample

**List all VMs (operation parameter is optional, defaults to "list"):**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Vms" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

**Get a specific VM:**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.Vms" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --entityId <entity-id>"
```

### VcDatacenters Sample

**List all vCenter datacenters (operation parameter is optional, defaults to "list"):**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.VcDatacenters" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

**Get a specific vCenter datacenter:**
```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.entities.VcDatacenters" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --entityId <entity-id>"
```



## Operations Details

### List Operation

- **Purpose**: Retrieves all entities of a specific type
- **API Method**: `list<EntityType>()` (e.g., `listHosts()`, `listClusters()`, `listDatastores()`)
- **Returns**: List of `EntityId` or `EntityIdWithTime` objects containing entity identifiers
- **Use Case**: Get an overview of all entities, discover entity IDs

### Get Operation

- **Purpose**: Retrieves detailed information about a specific entity
- **API Method**: `get<EntityType>(entityId)` (e.g., `getHost(entityId)`, `getCluster(entityId)`, `getDatastore(entityId)`)
- **Returns**: `Entity` or specific entity type object (e.g., `Cluster`) with complete entity details
- **Use Case**: Get full details about a specific entity
- **Required Parameter**: `--entityId`

## API Architecture

All entity samples follow this architecture:

1. **Authentication**: Create an `ApiClient` using `VcfOpsNetworksClientFactory.createClient()`
2. **Stub Creation**: Create entity-specific stub using `apiClient.createStub(<EntityStubClass>.class)`
3. **API Invocation**: Call the appropriate method on the stub
4. **Async Execution**: Use `.invoke().get()` to execute the async operation
5. **Result Processing**: Display or process the returned data

### Code Example

```java
// Load keystore (optional, for SSL/TLS certificate validation)
KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

// Create API client
VcfOpsNetworksClientFactory factory = new VcfOpsNetworksClientFactory();
ApiClient apiClient = factory.createClient(hostName, username, password, keyStore);

// Create entity stub
com.vmware.sdk.ops.networks.entities.Hosts hostsStub =
    apiClient.createStub(com.vmware.sdk.ops.networks.entities.Hosts.class);

// List hosts (returns EntityIdWithTime)
List<EntityIdWithTime> hosts = hostsStub.listHosts().invoke().get().getResults();

// Get specific host (returns Host object)
Host host = hostsStub.getHost("host-001").invoke().get();
```

### Clusters Example (with specific return types)

```java
// Load keystore (optional, for SSL/TLS certificate validation)
KeyStore keyStore = loadKeystoreOrCreateEmpty(trustStorePath);

// Create API client
VcfOpsNetworksClientFactory factory = new VcfOpsNetworksClientFactory();
ApiClient apiClient = factory.createClient(hostName, username, password, keyStore);

// Create clusters stub
com.vmware.sdk.ops.networks.entities.Clusters clustersStub =
    apiClient.createStub(com.vmware.sdk.ops.networks.entities.Clusters.class);

// List clusters (returns EntityIdWithTime)
List<EntityIdWithTime> clusters = clustersStub.listClusters().invoke().get().getResults();

// Get specific cluster (returns Cluster object)
Cluster cluster = clustersStub.getCluster("cluster-001").invoke().get();
```

## Entity Types

| Sample Class | Entity Type | Stub Class | Operations | Description |
|--------------|-------------|------------|------------|-------------|
| `Hosts.java` | Host | `com.vmware.sdk.ops.networks.entities.Hosts` | list, get | ESXi hosts |
| `Datastores.java` | Datastore | `com.vmware.sdk.ops.networks.entities.Datastores` | list, get | Storage datastores |
| `Clusters.java` | Cluster | `com.vmware.sdk.ops.networks.entities.Clusters` | list, get | Compute clusters |
| `Vms.java` | VM | `com.vmware.sdk.ops.networks.entities.Vms` | list, get | Virtual machines |
| `VcDatacenters.java` | VcDatacenter | `com.vmware.sdk.ops.networks.entities.VcDatacenters` | list, get | vCenter datacenters |

## Error Handling

All samples include comprehensive error handling:

- **ExecutionException**: API call failures
- **InterruptedException**: Operation interruptions
- **General Exception**: Unexpected errors

Example error output:

```
15:33:12.123 ERROR - Execution error occurred: Failed to connect to host: Connection refused
```

## Customization

You can modify the default values in each sample's Java file. The `operation` parameter is initialized as `null` and set to `"list"` by default via the `initUnsetOptionalParameters()` method if not provided:

```java
public static String hostName = "hostName";
public static String username = "username";
public static String password = "password";
public static String trustStorePath = null;
public static String operation = null;  // Optional, defaults to "list" via initUnsetOptionalParameters()
public static String entityId = null;
```

## Best Practices

1. **Secure Credentials**: Use environment variables or secure vaults for credentials (never hardcode)
2. **Optional Parameters**: The `--operation` parameter is optional and defaults to `"list"` if not specified
3. **Handle Pagination**: For large result sets, implement pagination logic
4. **Cache Entity IDs**: Store entity IDs from list operations for subsequent get operations
5. **Error Recovery**: Implement retry logic for API calls in production environments
6. **Resource Cleanup**: Ensure API clients are properly closed after use
7. **SSL/TLS Certificates**: Use `--trustStorePath` for custom certificates in production environments

## Troubleshooting

### Connection Issues

```
ERROR - Failed to connect to host: Connection refused
```
**Solution**: Verify the hostname and ensure the VCF Operations for networks instance is running and accessible.

### Authentication Failures

```
ERROR - Authentication failed: Invalid credentials
```
**Solution**: Verify your username and password, or check if your authentication token is valid.

### Entity Not Found

```
ERROR - Entity not found: host-999
```
**Solution**: Verify the entity ID exists by running a list operation first.

## Related Samples

- **Groups Samples**: `../groups/` - Application and discovered application management
- **Infrastructure Samples**: `../infra/` - Node and watermark management
- **Info Samples**: `../info/` - Version information
