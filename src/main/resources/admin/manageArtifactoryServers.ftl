[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.ManageArtifactoryServersAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.ManageArtifactoryServersAction" --]

<html>
<head>
    <title>Manage Artifactory Servers</title>
    <meta name="decorator" content="adminpage">
</head>

<body>

<h1>Manage Artifactory Servers</h1>

<p>You can use this page to add, edit and delete Artifactory server configurations.</p>

[@ww.action name="existingArtifactoryServer" executeResult="true" /]

<br/>

[@ww.action name="configureArtifactoryServer!default" executeResult="true" /]

</body>
</html>