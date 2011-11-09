[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.ExistingArtifactoryServerAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.ExistingArtifactoryServerAction" --]
<table id="existingArtifactoryServer" class="grid maxWidth">
    <thead>
    <tr>
        <th>Artifactory Server URL</th>
        <th>Username</th>
        <th>Timeout</th>
        <th>Operations</th>
    </tr>
    </thead>
[#foreach serverConfig in paginationSupport.page]
    <tr>
        <td>
        ${serverConfig.url}
        </td>
        <td>
        ${serverConfig.username}
        </td>
        <td>
        ${serverConfig.timeout}
        </td>
        <td class="operations">
            <a id="editServer-${serverConfig.id}" href="[@ww.url action='editServer' serverId=serverConfig.id/]">
                Edit
            </a>
            |
            <a id="deleteServer-${serverConfig.id}"
               href="[@ww.url action='deleteServer' serverId=serverConfig.id/]"
               onclick='return confirm("Are you sure you wish to remove this configuration?")'>
                Delete
            </a>
        </td>
    </tr>
[/#foreach]
</table>

[@cp.entityPagination actionUrl='${req.contextPath}/admin/manageArtifactoryServers.action?' paginationSupport=paginationSupport /]
