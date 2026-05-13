# Version Sample

This sample demonstrates how to retrieve version information from VCF Operations for networks using the Java SDK.

## Overview

The Version sample shows how to:
- Create an authenticated API client using `VcfOpsNetworksClientFactory`
- Retrieve authentication tokens (automatically or using a pre-existing token)
- Call the Version API to get version information
- Display version details including API version and version string

## Prerequisites

- VCF Operations for networks instance (hostname/IP address)
- Valid credentials (username and password) - BOTH REQUIRED
- Java 11 or later
- Maven 3.6 or later

## Running the Sample

The sample will automatically retrieve a new authentication token using basic authentication:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.info.Version" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password>"
```

### With Trust Store (Optional)

If you need to specify a trust store for SSL/TLS certificate validation:

```bash
./mvnw -s settings.xml -Dsdk.repo="vcf-sdk-java" exec:java \
  -Dexec.mainClass="com.vmware.sdk.samples.ops.networks.info.Version" \
  -Dexec.args="--hostName <hostname-or-ip> --username <username> --password <password> --trustStorePath /path/to/truststore.jks"
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--hostName` | Yes | VCF Operations for networks hostname or IP |
| `--username` | Yes | Username for authentication |
| `--password` | Yes | Password for authentication |
| `--trustStorePath` | No | Trust store path for SSL/TLS certificate validation (OPTIONAL) |


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
- **ExecutionException**: API call failures
- **InterruptedException**: Operation interruptions
- **General Exceptions**: Unexpected errors

All errors are logged with appropriate messages and the program exits with a non-zero status code.

## Customization

You can modify the default values in the `Version.java` file:

```java
public static String hostName = "hostName";
public static String username = "username";
public static String password = "password";
public static String token = null;
```

These defaults will be used if command-line arguments are not provided.

## Troubleshooting

### "Wrong number of command line parameters"

This error occurs when using `=` instead of space for parameter values:

### Connection Errors

- Verify the hostname/IP address is correct
- Ensure the VCF Operations for networks instance is running and accessible
- Check network connectivity and firewall rules

### Authentication Errors

- Verify username and password are correct
- Ensure the user has appropriate permissions
- Check if the token (if provided) is valid and not expired

## Related Files

- `Version.java` - Main sample class
- `VcfOpsNetworksClientFactory.java` - Utility class for creating authenticated API clients
- `AuthenticationHeaderAppender.java` - Custom authentication header appender for token-based auth


