# Groups API Samples

This directory contains samples for VCF Operations for networks Groups APIs.

## Available Samples

| Sample | Description | Operations |
|--------|-------------|------------|
| `Applications.java` | Manage user-defined applications | list, get, create, delete |
| `DiscoveredApplications.java` | View automatically discovered applications | list (read-only) |
| `TieredApplications.java` | Manage tiered applications with tiers | create, add tier, get tier, delete tier, delete |

For detailed information, see the individual sample sections below.

---

# Applications Sample

This sample demonstrates a complete workflow for managing user-defined Applications in VCF Operations for networks using the Java SDK.

## Overview

The Applications sample demonstrates a complete workflow that:
- Creates an authenticated API client using `VcfOpsNetworksClientFactory`
- Lists all applications (initial state)
- Creates a new application
- Lists all applications again (demonstrating the newly created application)
- Gets the newly created application by its ID
- Deletes the created application
- Lists all applications one final time (confirming deletion)

This workflow-based approach provides a comprehensive demonstration of the full application lifecycle.

## Prerequisites

- VCF Operations for networks instance (hostname/IP address)
- Valid credentials (username and password) - BOTH REQUIRED
- Java 11 or later
- Maven 3.6 or later

## Running the Sample

The sample executes a complete workflow automatically. Simply provide the required connection parameters:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.Applications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.Applications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |

## Workflow Steps

The sample executes the following steps in sequence:

### Step 1: List Applications (Initial State)
- **API Method**: `listApplications()`
- **Purpose**: Shows the initial state of applications before any modifications
- **Returns**: List of `EntityId` objects containing application identifiers
- **Display**: Shows total count and details for each application (entity ID and type)

### Step 2: Create Application
- **API Method**: `addApplication(ApplicationRequest)`
- **Creates**: A test application named "Test SDK app1"
- **Returns**: `Application` object with the created application details
- **Display**: Shows full details of the newly created application

### Step 3: List Applications (After Creation)
- **API Method**: `listApplications()`
- **Purpose**: Demonstrates that the newly created application appears in the list
- **Returns**: List of `EntityId` objects (now includes the newly created application)
- **Display**: Shows the updated list with the new application

### Step 4: Get Application by ID
- **API Method**: `getApplicationById(applicationId)`
- **Purpose**: Retrieves the full details of the newly created application
- **Returns**: `Application` object with detailed information
- **Display**: Shows complete application details

### Step 5: Delete Application
- **API Method**: `deleteApplication(applicationId)`
- **Purpose**: Removes the test application to clean up
- **Returns**: Void (deletion confirmation)
- **Note**: This ensures no test data is left behind

### Step 6: List Applications (After Deletion)
- **API Method**: `listApplications()`
- **Purpose**: Confirms that the deleted application no longer appears in the list
- **Returns**: List of `EntityId` objects (back to the original state)
- **Display**: Shows the final list without the deleted application

## Authentication

The samples use the `VcfOpsNetworksClientFactory` which provides:

- **Automatic Token Management**: Tokens are automatically retrieved and managed internally
- **Token Refresh**: Expired tokens are automatically refreshed without user intervention
- **Thread-Safe**: Token operations are thread-safe for concurrent usage
- **Secure Credential Handling**: Passwords are stored securely using char arrays
- **Retry Logic**: Built-in retry mechanism for token acquisition failures

**Note:** You no longer need to manually fetch or provide tokens. Simply provide username and password, and the factory handles all token operations automatically.


## Error Handling

The sample includes comprehensive error handling for:

### ExecutionException
- API call failures (network errors, authentication failures)
- Invalid application IDs
- Permission errors

### InterruptedException
- Operation interruptions (user cancellation)
- Timeout scenarios

### Validation Errors
- Missing required parameters (e.g., `--applicationId` for get/delete operations)
- Invalid operation names

All errors are logged with appropriate messages and the program exits with a non-zero status code.

## Customization

You can modify the default values in the `Applications.java` file:

```java
public static String hostName = "hostName";
public static String username = "username";
public static String password = "password";
public static String trustStorePath = null;
```

### Customizing the Create Operation

To change the application name when creating, modify the `createApplication()` method:

```java
ApplicationRequest request = new ApplicationRequest();
request.setName("Your Custom Application Name");
```

## Troubleshooting

### "Wrong number of command line parameters"

This error occurs when using `=` instead of space for parameter values:

❌ **Wrong:**
```bash
--operation=list
```

✅ **Correct:**
```bash
--operation list
```

### "Application ID is required for get/delete operation"

Make sure to provide the `--applicationId` parameter:

```bash
--args="--hostName <hostname-or-ip> --username <username> --password <password> --operation get --applicationId app-123"
```

### Connection Errors

- Verify the hostname/IP address is correct
- Ensure the VCF Operations for networks instance is running and accessible
- Check network connectivity and firewall rules
- Verify the instance is using HTTPS (default port 443)

### Authentication Errors

- Verify username and password are correct
- Ensure the user account is active and not locked
- Check if the user has appropriate permissions to access the Applications API
- Verify the authentication domain (e.g., `@local` for local users)

### Application Not Found Errors

- Verify the application ID exists in the system
- Use the `list` operation to get valid application IDs
- Ensure the application hasn't been deleted

## Security Considerations

- **Never hardcode credentials**: Use command-line arguments or environment variables
- **Protect tokens**: Treat tokens like passwords - they provide full API access
- **Application lifecycle**: The create operation automatically cleans up test data by deleting the created application
- **Permission checks**: Ensure your user has appropriate permissions for application management
- **HTTPS only**: Always use HTTPS connections to protect credentials in transit

## Code Structure

### Main Classes and Methods

| Method | Description |
|--------|-------------|
| `main()` | Entry point - parses arguments and executes the complete workflow |
| `executeWorkflow()` | Orchestrates the complete workflow (list, create, list, get, delete, list) |
| `listApplications()` | API call to retrieve all applications |
| `getApplication()` | API call to retrieve a specific application by ID |
| `createApplication()` | API call to create a new application |
| `deleteApplication()` | API call to delete an application |
| `displayApplicationsList()` | Formats and displays application list |
| `displayApplicationInfo()` | Formats and displays detailed application info |

### Model Classes Used

| Class | Purpose |
|-------|---------|
| `EntityId` | Contains application identifier and type |
| `Application` | Contains detailed application information |
| `ApplicationRequest` | Request object for creating applications |
| `PagedListResponse` | Response wrapper for list operations |

## Related Files

- `Applications.java` - Main sample class for application management
- `VcfOpsNetworksClientFactory.java` - Utility class for creating authenticated API clients and managing tokens
- `../token/FetchToken.java` - Example of retrieving authentication tokens
- `../info/Version.java` - Example of using tokens with API calls

## Notes

- All operations use the `VcfOpsNetworksClientFactory` for consistent authentication and client creation
- Token caching and expiry checking are handled automatically by `VcfOpsNetworksClientFactory`
- The workflow includes automatic cleanup (deletion) to avoid leaving test data
- All parameters use the format `--parameterName value` (space-separated, not `=`)
- The sample demonstrates proper error handling and logging practices
- Application IDs are automatically assigned by the system when creating applications
- The workflow executes all steps sequentially to demonstrate the complete application lifecycle

---

# Discovered Applications Sample

This sample demonstrates a complete workflow for retrieving and analyzing Discovered Applications in VCF Operations for networks using the Java SDK.

## Overview

The Discovered Applications sample demonstrates a complete workflow that:
- Creates an authenticated API client using `VcfOpsNetworksClientFactory`
- Lists discovered applications with SERVICE_NOW discovery type
- Lists discovered applications with FLOW_BASED_DISCOVERY discovery type
- Analyzes and summarizes the discovered applications from both sources

## What are Discovered Applications?

Discovered Applications are applications that are automatically identified by VCF Operations for networks through network traffic analysis and monitoring. Key characteristics:

- **Automatically Detected**: Identified through network flow analysis and integrations
- **Read-Only**: Cannot be created, modified, or deleted through the API
- **Real Traffic**: Represent actual network traffic patterns in your environment
- **Dynamic**: Updated as network traffic patterns change and integrations sync

Unlike user-defined applications (see `Applications.java`), discovered applications reflect what VCF Operations for networks observes in your network rather than what you manually configure.

## Prerequisites

- VCF Operations for networks instance (hostname/IP address)
- Valid credentials (username and password) - BOTH REQUIRED
- Java 11 or later
- Maven 3.6 or later
- Network traffic data collection enabled in your environment (for FLOW_BASED_DISCOVERY)
- ServiceNow integration configured (for SERVICE_NOW discovery type, optional)

## Running the Sample

The sample executes a complete workflow automatically, querying both SERVICE_NOW and FLOW_BASED_DISCOVERY discovery types:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.DiscoveredApplications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.DiscoveredApplications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |

## Workflow Steps

The sample executes the following steps in sequence:

### Step 1: List Discovered Applications (SERVICE_NOW)
- **API Method**: `getDiscoveredApplications()`
- **Discovery Type**: `SERVICE_NOW` - retrieves applications discovered through ServiceNow CMDB integration
- **Returns**: List of `EntityId` objects containing discovered application identifiers
- **Read-Only**: This is a query-only operation
- **Display**: Shows total count and details for each discovered application

### Step 2: List Discovered Applications (FLOW_BASED_DISCOVERY)
- **API Method**: `getDiscoveredApplications()`
- **Discovery Type**: `FLOW_BASED_DISCOVERY` - retrieves applications automatically identified through network flow analysis
- **Granularity Parameter**: Required for FLOW_BASED_DISCOVERY. Allowed values: `FINE`, `MEDIUM`, `COARSE`. This sample uses `FINE` for maximum detail.
- **Returns**: List of `EntityId` objects containing discovered application identifiers
- **Error Handling**: Gracefully handles errors if FLOW_BASED_DISCOVERY is not configured or fails
- **Display**: Shows total count and details for each discovered application

### Step 3: Analyze Discovered Applications
- **Purpose**: Provides a summary analysis of discovered applications from both sources
- **Analysis Includes**:
  - Total count for each discovery type
  - Combined total count
  - Distinct entity types for each source
  - Key characteristics of discovered applications

### API Call Details

The sample makes the following API calls:

**For SERVICE_NOW:**
```java
discoveredApplicationsStub.getDiscoveredApplications()
        .discoveryType("SERVICE_NOW")
        .invoke()
        .get();
```

**For FLOW_BASED_DISCOVERY:**
```java
discoveredApplicationsStub.getDiscoveredApplications()
        .discoveryType("FLOW_BASED_DISCOVERY")
        .granularity("FINE")  // Required parameter: FINE, MEDIUM, or COARSE
        .invoke()
        .get();
```

**Parameters:**
- `discoveryType`: Specifies the type of discovery mechanism (`SERVICE_NOW` or `FLOW_BASED_DISCOVERY`)
- `granularity`: Required for `FLOW_BASED_DISCOVERY` only. Allowed values: `FINE`, `MEDIUM`, `COARSE`

## Discovered vs User-Defined Applications

| Feature | Discovered Applications | User-Defined Applications |
|---------|------------------------|---------------------------|
| **Source** | Automatically detected from integrations/traffic | Manually created by users |
| **API Operations** | List only (read-only) | List, Get, Create, Delete |
| **Modification** | Cannot be modified | Can be created and deleted |
| **Purpose** | Visibility into actual network traffic | Custom application definitions |
| **Sample** | `DiscoveredApplications.java` | `Applications.java` |

## Customization

You can modify the default values in the `DiscoveredApplications.java` file:

```java
public static String hostName = "hostName";
public static String username = "username";
public static String password = "password";
public static String trustStorePath = null;
```

### Changing Granularity for FLOW_BASED_DISCOVERY

To use a different granularity level for FLOW_BASED_DISCOVERY, modify the `listDiscoveredApplications()` method:

```java
if ("FLOW_BASED_DISCOVERY".equals(discoveryType)) {
    invocation = invocation.granularity("MEDIUM");  // Change to FINE, MEDIUM, or COARSE
}
```

**Available Granularity Values:**
- `FINE` - Maximum detail (default in this sample)
- `MEDIUM` - Medium level of detail
- `COARSE` - Coarse level of detail

**Available Discovery Types:**
- `SERVICE_NOW` - Applications from ServiceNow CMDB integration
- `FLOW_BASED_DISCOVERY` - Applications automatically identified through network flow analysis

## Understanding Discovered Applications

### How Discovery Works

1. **Traffic Collection**: VCF Operations for networks collects network flow data
2. **Pattern Analysis**: Network traffic patterns are analyzed to identify applications
3. **Integration Sources**: Applications can be discovered through various sources:
   - **SERVICE_NOW**: Applications discovered through ServiceNow CMDB integration
   - Network traffic analysis
   - Port and protocol identification
   - Known application signatures
4. **Application Identification**: Applications are identified based on:
   - ServiceNow CMDB data
   - Port numbers
   - Protocol signatures
   - Traffic patterns
   - Known application characteristics
5. **Continuous Updates**: The list of discovered applications updates as traffic patterns change and integrations sync

### Use Cases

- **Network Visibility**: Understand what applications are actually running in your environment
- **Compliance Auditing**: Verify expected applications and identify unexpected ones
- **Capacity Planning**: Identify high-traffic applications for resource allocation
- **Security Analysis**: Detect unauthorized or suspicious applications
- **Baseline Creation**: Establish normal application behavior patterns
- **CMDB Integration**: Sync discovered applications with ServiceNow CMDB

## Troubleshooting

### No Discovered Applications Returned

If the sample returns no discovered applications:

1. **Verify ServiceNow Integration**: Ensure ServiceNow CMDB integration is configured and active
2. **Check Data Collection**: Ensure network traffic collection is enabled
3. **Wait for Discovery**: Application discovery requires time to analyze traffic patterns
4. **Check Permissions**: Ensure your user has permissions to view discovered applications
5. **Verify Discovery Type**: Confirm the discovery type matches your environment configuration

### Connection Errors

- Verify the hostname/IP address is correct
- Ensure the VCF Operations for networks instance is running and accessible
- Check network connectivity and firewall rules
- Verify the instance is using HTTPS (default port 443)

### Authentication Errors

- Verify username and password are correct
- Ensure the user account is active and not locked
- Check if the user has appropriate permissions to access the Discovered Applications API
- Verify the authentication domain (e.g., `@local` for local users)

## Code Structure

### Main Classes and Methods

| Method | Description |
|--------|-------------|
| `main()` | Entry point - parses arguments and executes the complete workflow |
| `executeWorkflow()` | Orchestrates the complete workflow (list SERVICE_NOW, list FLOW_BASED_DISCOVERY, analyze) |
| `listDiscoveredApplications()` | API call to retrieve discovered applications with a specific discovery type |
| `displayDiscoveredApplicationsList()` | Formats and displays discovered application list |
| `analyzeDiscoveredApplications()` | Analyzes and summarizes discovered applications from both sources |

### Model Classes Used

| Class | Purpose |
|-------|---------|
| `EntityId` | Contains discovered application identifier and type |
| `PagedListResponse` | Response wrapper for list operations |

## Notes

- All operations use the `VcfOpsNetworksClientFactory` for consistent authentication and client creation
- Token caching and expiry checking are handled automatically by `VcfOpsNetworksClientFactory`
- Discovered applications are read-only and reflect actual network traffic
- The workflow queries both `SERVICE_NOW` and `FLOW_BASED_DISCOVERY` discovery types
- FLOW_BASED_DISCOVERY requires a granularity parameter (FINE, MEDIUM, or COARSE) - this sample uses FINE
- The sample includes error handling for cases where FLOW_BASED_DISCOVERY is not configured or fails
- The sample demonstrates proper error handling and logging practices
- All parameters use the format `--parameterName value` (space-separated, not `=`)
- Discovery requires active network traffic collection and analysis
- The number of discovered applications may change over time as traffic patterns evolve
- ServiceNow integration must be configured for the SERVICE_NOW discovery type to return results
- The workflow executes all steps sequentially to demonstrate querying and analyzing discovered applications from multiple sources

---

# Tiered Applications Sample

This sample demonstrates a complete workflow for managing Tiered Applications in VCF Operations for networks using the Java SDK.

## Overview

The Tiered Applications sample demonstrates a complete workflow that:
- Creates an authenticated API client using `VcfOpsNetworksClientFactory`
- Creates a new application
- Adds a tier to the application with membership criteria
- Gets the tier information to verify creation
- Deletes the tier from the application
- Deletes the application
- Lists all applications to confirm deletion

Tiered Applications are applications that are organized into multiple tiers (e.g., web tier, application tier, database tier). This sample demonstrates how to manage the complete lifecycle of a tiered application including tier management.

## Prerequisites

- VCF Operations for networks instance (hostname/IP address)
- Valid credentials (username and password) - BOTH REQUIRED
- Java 11 or later
- Maven 3.6 or later

## Running the Sample

The sample executes a complete workflow automatically. Simply provide the required connection parameters:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.TieredApplications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.groups.TieredApplications" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |

## Workflow Steps

The sample executes the following steps in sequence:

### Step 1: Create Application
- **API Method**: `addApplication(ApplicationRequest)`
- **Creates**: A test application named "Test Tiered Application"
- **Error Handling**: If an application with the same name already exists, it will be automatically deleted before creating a new one
- **Returns**: `Application` object with the created application details
- **Display**: Shows full details of the newly created application

### Step 2: Add Tier to Application
- **API Method**: `addTier(applicationId, TierRequest)`
- **Creates**: A tier named "Web Tier" with membership criteria
- **TierRequest Configuration**:
  - Sets tier name
  - Configures `GroupMembershipCriteria` with `SearchMembershipCriteria`
  - Sets entity type (e.g., "VirtualMachine")
  - Sets filter criteria for tier membership
- **Returns**: `Tier` object with the created tier details
- **Display**: Shows tier ID after creation

### Step 3: Get Tier Information
- **API Method**: `getApplicationTier(applicationId, tierId)`
- **Purpose**: Retrieves the full details of the newly created tier
- **Returns**: `Tier` object with detailed tier information
- **Display**: Shows complete tier details

### Step 4: Delete Tier from Application
- **API Method**: `deleteTier(applicationId, tierId)`
- **Purpose**: Removes the tier from the application
- **Returns**: Void (deletion confirmation)

### Step 5: Delete Application
- **API Method**: `deleteApplication(applicationId)`
- **Purpose**: Removes the test application to clean up
- **Returns**: Void (deletion confirmation)
- **Note**: This ensures no test data is left behind

### Step 6: List Applications
- **API Method**: `listApplications()`
- **Purpose**: Confirms that the deleted application no longer appears in the list
- **Returns**: List of `EntityId` objects (back to the original state)
- **Display**: Shows the final list without the deleted application

## Tier Membership Criteria

Tiers use `GroupMembershipCriteria` to define which entities belong to the tier. The sample demonstrates:

### SearchMembershipCriteria
- **Entity Type**: Specifies the type of entities (e.g., "VirtualMachine")
- **Filter**: Defines search criteria using filter expressions (e.g., "security_groups.entity_id = '18230:82:604573173'")

### GroupMembershipCriteria
- **Membership Type**: Set to "SearchMembershipCriteria"
- **SearchMembershipCriteria**: Contains the search criteria for tier membership

## Authentication

The samples use the `VcfOpsNetworksClientFactory` which provides:

- **Automatic Token Management**: Tokens are automatically retrieved and managed internally
- **Token Refresh**: Expired tokens are automatically refreshed without user intervention
- **Thread-Safe**: Token operations are thread-safe for concurrent usage
- **Secure Credential Handling**: Passwords are stored securely using char arrays
- **Retry Logic**: Built-in retry mechanism for token acquisition failures

**Note:** You no longer need to manually fetch or provide tokens. Simply provide username and password, and the factory handles all token operations automatically.

## Error Handling

The sample includes comprehensive error handling for:

### ExecutionException
- API call failures (network errors, authentication failures)
- Invalid application or tier IDs
- Permission errors
- Duplicate application names (automatically handled by deleting existing application)

### InterruptedException
- Operation interruptions (user cancellation)
- Timeout scenarios

### ApiError Handling
- **Duplicate Application Names**: If an application with the same name already exists (status code 400), the sample automatically:
  1. Finds the existing application by name
  2. Deletes it
  3. Retries creating the application

All errors are logged with appropriate messages and the program exits with a non-zero status code.

## Customization

You can modify the default values in the `TieredApplications.java` file:

```java
public static String hostName = "hostName";
public static String username = "username";
public static String password = "password";
public static String trustStorePath = null;
```

### Customizing the Application Name

To change the application name when creating, modify the `createApplication()` method:

```java
ApplicationRequest request = new ApplicationRequest();
request.setName("Your Custom Application Name");
```

### Customizing Tier Configuration

To customize tier properties, modify the `addTierToApplication()` method:

```java
// Customize SearchMembershipCriteria
SearchMembershipCriteria searchMembershipCriteria = new SearchMembershipCriteria();
searchMembershipCriteria.setEntityType("YourEntityType");
searchMembershipCriteria.setFilter("your_custom_filter_expression");

// Customize tier name
TierRequest tierRequest = new TierRequest();
tierRequest.setName("Your Custom Tier Name");
```

## Troubleshooting

### "Application with same name already exists"

The sample automatically handles this by deleting the existing application. If you see a warning message about this, it's expected behavior and the sample will continue after cleanup.

### Connection Errors

- Verify the hostname/IP address is correct
- Ensure the VCF Operations for networks instance is running and accessible
- Check network connectivity and firewall rules
- Verify the instance is using HTTPS (default port 443)

### Authentication Errors

- Verify username and password are correct
- Ensure the user account is active and not locked
- Check if the user has appropriate permissions to access the Applications and Tiers APIs
- Verify the authentication domain (e.g., `@local` for local users)

### Tier Creation Errors

- Verify the entity type is valid (e.g., "VirtualMachine")
- Check that the filter expression syntax is correct
- Ensure the referenced entities exist in the system
- Verify permissions for creating tiers

## Security Considerations

- **Never hardcode credentials**: Use command-line arguments or environment variables
- **Protect tokens**: Treat tokens like passwords - they provide full API access
- **Application lifecycle**: The create operation automatically cleans up test data by deleting the created application and tiers
- **Permission checks**: Ensure your user has appropriate permissions for application and tier management
- **HTTPS only**: Always use HTTPS connections to protect credentials in transit

## Code Structure

### Main Classes and Methods

| Method | Description |
|--------|-------------|
| `main()` | Entry point - parses arguments and executes the complete workflow |
| `executeWorkflow()` | Orchestrates the complete workflow (create app, add tier, get tier, delete tier, delete app, list apps) |
| `createApplication()` | API call to create a new application (handles duplicate names) |
| `deleteExistingApplicationByName()` | Helper method to find and delete existing application by name |
| `addTierToApplication()` | API call to add a tier to an application |
| `getApplicationTier()` | API call to retrieve a specific tier by ID |
| `deleteTierFromApplication()` | API call to delete a tier from an application |
| `deleteApplication()` | API call to delete an application |
| `listApplications()` | API call to retrieve all applications |
| `displayApplicationInfo()` | Formats and displays detailed application info |
| `displayTierInfo()` | Formats and displays detailed tier info |
| `displayApplicationsList()` | Formats and displays application list |

### Model Classes Used

| Class | Purpose |
|-------|---------|
| `Application` | Contains detailed application information |
| `ApplicationRequest` | Request object for creating applications |
| `Tier` | Contains detailed tier information |
| `TierRequest` | Request object for creating tiers |
| `GroupMembershipCriteria` | Defines membership criteria for tiers |
| `SearchMembershipCriteria` | Defines search-based membership criteria |
| `EntityId` | Contains application identifier and type |
| `PagedListResponse` | Response wrapper for list operations |
| `ApiError` | Error response from API calls |

## Related Files

- `TieredApplications.java` - Main sample class for tiered application management
- `Applications.java` - Sample for basic application management
- `VcfOpsNetworksClientFactory.java` - Utility class for creating authenticated API clients and managing tokens

## Notes

- All operations use the `VcfOpsNetworksClientFactory` for consistent authentication and client creation
- Token caching and expiry checking are handled automatically by `VcfOpsNetworksClientFactory`
- The workflow includes automatic cleanup (deletion) to avoid leaving test data
- All parameters use the format `--parameterName value` (space-separated, not `=`)
- The sample demonstrates proper error handling and logging practices
- Application IDs and tier IDs are automatically assigned by the system when creating
- The workflow executes all steps sequentially to demonstrate the complete tiered application lifecycle
- Duplicate application names are automatically handled by deleting existing applications before creating new ones
- Tiers require `GroupMembershipCriteria` with `SearchMembershipCriteria` to define membership rules
