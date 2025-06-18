This directory contains samples for managing vCenter signing certificate:

The samples were tested against vSphere 7.0.3

Author: Andrew Gormley <agormley@vmware.com>
Date: 09/15/2021

### SigningCertificate Get operations

| Sample                                                                            | Description                                                                                                                                               |
|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.certificates.GetSigningCertificate.java | Demonstrates SigningCertificate Get operation which displays the active signing certificate chain and certificate chains used to verify token signatures. |

### SigningCertificate Refresh operations

| Sample                                                                                | Description                                                                                                                                                                       |
|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.management.certificates.RefreshSigningCertificate.java | Demonstrates SigningCertificate Refresh operation which creates a new private key and certificate chain issued by VMCA for token signing, and displays the new certificate chain. |

### Testbed Requirement:

    One (1) vCenter Server
    The username being used to run the sample should have the Administrator Role.
