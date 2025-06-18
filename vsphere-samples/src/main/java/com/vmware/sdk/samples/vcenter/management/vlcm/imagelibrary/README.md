ImageLibraryOperations.java contains samples for making vSphere lifecyle manager Image Library API calls.

### Testbed Requirement:

    - vCenter Server >= 9.0.0+
    - Datacenter in the vCenter Server
    - A cluster in the datacenter
    - All the vlcm depots required for the desired image should be present

### Creating and editing an image

| Sample                                                              | Description                                                                        |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------|
| createNewImage()                                                    | Create a new image in the Image Library                                            |
| renameImage(imageId)                                                | Rename the image identified by the imageId in the Image Library                    |
| deleteImage(imageId)                                                | Delete an existing image in the Image Library                                      |
| getCurrentImageAssignedToCluster(clusterId)                         | Get the currently assigned image on the cluster                                    |


### Running assign and scan tasks

| Sample                                    | Description                                                       |
|-------------------------------------------|-------------------------------------------------------------------|
| assignImageToCluster(imageId. clusterId)  | Assign the image to the cluster                                   |
| complianceScanAtVcenter()                 | Run a compliance scan on all clusters in the vCenter              |
| getTaskInfo(String taskId)                | Tries to poll the result of the task till it fails or succeeds    |
