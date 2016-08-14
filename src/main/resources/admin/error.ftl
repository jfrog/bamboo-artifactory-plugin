[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.ArtifactoryErrorAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.ArtifactoryErrorAction" --]

<html>
<head>
    <title>[@ww.text name='error.title' /]</title>
    <meta name="decorator" content="errorDecorator"/>
</head>

<body>
[@ui.header pageKey='error.heading' /]

[#if formattedActionErrors?has_content]
    [@ui.messageBox type='warning']
        [#list formattedActionErrors as error]
        <p>${error}</p>
        [/#list]
    [/@ui.messageBox]
[/#if]
</body>
</html>