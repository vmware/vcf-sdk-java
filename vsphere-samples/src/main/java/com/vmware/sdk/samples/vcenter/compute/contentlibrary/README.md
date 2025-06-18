This directory contains samples for Content Library APIs:

| Sample                                                                                              | Description                                                                                                       |
|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.crud.LibraryCrud.java                         | CRUD operations on a content library                                                                              |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.contentupdate.ContentUpdate.java              | Updating content of a content library item                                                                        |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.isomount.IsoMount.java                        | Content library ISO item mount and unmount workflow                                                               |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.ovfdeploy.DeployOvfTemplate.java              | Workflow to deploy an OVF library item to a resource pool                                                         |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.ovfimport.OvfImportExport.java                | Workflows to import an OVF package into a content library, and download of an OVF template from a content library |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.publishsubscribe.LibraryPublishSubscribe.java | Basic workflow to publish and subscribe content libraries                                                         |
| com.vmware.sdk.samples.vcenter.compute.contentlibrary.vmcapture.VmTemplateCapture.java              | Workflow to capture a virtual machine into a content library asa vm template                                      |

### Testbed Requirement:

    - 1 vCenter Server
    - 2 ESX hosts
    - 1 datastore
    - Some samples need a Content Library, VM or an OVF library item created as mentioned above