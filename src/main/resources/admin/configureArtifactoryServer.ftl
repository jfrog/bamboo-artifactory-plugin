[#if mode == 'edit' ]
    [#assign targetAction = 'updateServer']
    [#assign titleText = 'artifactory.server.edit' /]
<html>
<head><title>Update Artifactory Server</title></head>
<body>
[#else]
    [#assign targetAction = 'createArtifactoryServer']
<html>
<head><title>Create Artifactory Server</title></head>
<body>
[/#if]

[#assign cancelUri = '/admin/manageArtifactoryServers.action' /]

<div class="paddedClearer"></div>
[@ww.form action=targetAction
          titleKey='artifactory.server.details'
          descriptionKey='artifactory.server.description'
          submitLabelKey='global.buttons.update'
          cancelUri='/admin/manageArtifactoryServers.action'
          showActionErrors='true'
]
    [@ww.param name='buttons']
        [@ww.submit value=action.getText('global.buttons.test') name="sendTest" /]
    [/@ww.param]

    [@ui.bambooSection]
        [@ww.hidden name='serverId'/]
        [@ww.textfield labelKey="artifactory.server.url" name="url" required="true" autofocus=true/]
        [@ww.textfield labelKey='artifactory.server.username' name="username"/]
        [@ww.password labelKey='artifactory.server.password' name="password" showPassword='true'/]
        [@ww.textfield labelKey='artifactory.server.timeout' name="timeout" required="true"/]
    [/@ui.bambooSection]
[/@ww.form]
</body>
