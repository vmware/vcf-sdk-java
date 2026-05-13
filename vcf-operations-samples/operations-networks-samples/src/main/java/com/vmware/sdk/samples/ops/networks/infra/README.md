# Infrastructure API Samples

This directory contains samples for VCF Operations for networks Infrastructure APIs.

## Available Samples

| Sample | Description | Workflow |
|--------|-------------|----------|
| `Nodes.java` | List all nodes and retrieve specific node information | List nodes -> Get specific node details |
| `ExpandedNodes.java` | List all nodes with expanded information | List expanded nodes |
| `Watermark.java` | Manage VCF Watermark configuration | Get -> Save -> Get -> Update -> Get -> Delete -> Get |

## Prerequisites

- VCF Operations for networks instance (hostname/IP address)
- Valid credentials (username and password) - BOTH REQUIRED
- Java 11 or later
- Maven 3.6 or later

---

## Nodes Sample

This sample demonstrates a complete workflow for retrieving node information in VCF Operations for networks.

### Overview

The Nodes sample demonstrates a complete workflow that:
- Lists all nodes in the system
- Retrieves detailed information for a specific node (uses the first node from the list if no nodeId is provided)

### Running the Sample

The sample executes a complete workflow automatically:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.Nodes" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Specific Node ID (Optional)

If you want to retrieve a specific node by ID:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.Nodes" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --nodeId <node-id>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.Nodes" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |
| `--nodeId` | No | Specific node ID to retrieve (uses first node from the list if not provided) |

### Workflow Steps

The sample executes the following steps in sequence:

1. **List All Nodes**: Retrieves all nodes in the system with basic information
2. **Get Specific Node Details**: Retrieves detailed information for a specific node (first node if nodeId not provided)

---

## ExpandedNodes Sample

This sample demonstrates a complete workflow for retrieving expanded node information in VCF Operations for networks.

### Overview

The ExpandedNodes sample demonstrates a complete workflow that:
- Lists all nodes with expanded information (includes detailed properties and relationships not available in basic node listing)

Expanded nodes provide more comprehensive information compared to the basic node list, including additional properties and relationships.

### Running the Sample

The sample executes a complete workflow automatically:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.ExpandedNodes" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.ExpandedNodes" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |

### Workflow Steps

The sample executes the following step:

1. **List Expanded Nodes**: Retrieves all nodes with expanded information including detailed properties and relationships

---

## Watermark Sample

This sample demonstrates a complete workflow for managing VCF Watermark configuration in VCF Operations for networks.

### Overview

The Watermark sample demonstrates a complete workflow that:
- Gets the current VCF Watermark (initial state)
- Saves a new VCF Watermark configuration
- Gets the VCF Watermark again (demonstrating the saved configuration)
- Updates the VCF Watermark configuration
- Gets the VCF Watermark again (demonstrating the updated configuration)
- Deletes the VCF Watermark
- Gets the VCF Watermark one final time (confirming deletion)

This workflow-based approach provides a comprehensive demonstration of the full VCF Watermark lifecycle.

### Running the Sample

The sample executes a complete workflow automatically:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.Watermark" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.infra.Watermark" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |

### Workflow Steps

The sample executes the following steps in sequence:

1. **Get VCF Watermark (Initial State)**: Retrieves the current watermark configuration (handles 404 if no watermark exists)
2. **Save VCF Watermark**: Creates a new watermark configuration
3. **Get VCF Watermark (After Save)**: Verifies the saved watermark configuration
4. **Update VCF Watermark**: Updates the watermark configuration
5. **Get VCF Watermark (After Update)**: Verifies the updated watermark configuration
6. **Delete VCF Watermark**: Removes the watermark configuration
7. **Get VCF Watermark (After Deletion)**: Confirms deletion (handles 404 gracefully)

---

## Common Issues

### "Wrong number of command line parameters"

This error occurs when using `=` instead of space for parameter values:

❌ **Wrong:**
```bash
--hostName=<hostname-or-ip>
```

✅ **Correct:**
```bash
--hostName <hostname-or-ip>
```

### Connection Errors

- Verify the hostname/IP address is correct
- Ensure the VCF Operations for networks instance is running and accessible
- Check network connectivity and firewall rules
- Verify the instance is using HTTPS (default port 443)

### Authentication Errors

- Verify username and password are correct
- Ensure the user account is active and not locked
- Check if the user has appropriate permissions to access the Infrastructure APIs
- Verify the authentication domain (e.g., `@local` for local users)

### Watermark-Specific Errors

**404 Not Found:**
- The watermark sample handles 404 errors gracefully when no watermark exists initially
- After deletion, a 404 response confirms successful deletion

**Permission Errors:**
- Ensure your user has appropriate permissions for watermark management operations
- Some operations may require administrative privileges

---

## Related Files

- `VcfOpsNetworksClientFactory.java` - Utility class for creating authenticated API clients
- `AuthenticationHeaderAppender.java` - Custom authentication header appender for token-based auth
- `../info/Version.java` - Version API sample

---

## Notes

- All samples use the `VcfOpsNetworksClientFactory` for consistent authentication and client creation
- Token caching and expiry checking are handled automatically by `VcfOpsNetworksClientFactory`
- Samples include comprehensive error handling and logging
- All parameters use the format `--parameterName value` (space-separated, not `=`)
- The workflow executes all steps sequentially to demonstrate the complete lifecycle
- Watermark sample handles 404 errors gracefully when no watermark exists or after deletion
- Expanded nodes provide more detailed information than basic node listing

