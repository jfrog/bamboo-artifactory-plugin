[#if mode == 'edit' ]
    [#assign targetAction = 'updateServer']
    [#assign titleText = 'artifactory.server.edit' /]
<html>
<head><title>Update Artifactory Server</title></head>
<body>
    [#else]
        [#assign targetAction = 'createServer']
        [#assign titleText = 'artifactory.server.new' /]
[/#if]
[#assign cancelUri = '/admin/manageArtifactoryServers.action' /]

<h1>[@ww.text name=titleText labelKey=titleText /]</h1>
<div class="paddedClearer"></div>
[@ww.form action=targetAction submitLabelKey='global.buttons.update'
titleKey='artifactory.server.details'
cancelUri=cancelUri
descriptionKey='artifactory.server.description'
showActionErrors='true']
[@ww.param name='buttons']
[@ww.submit value="Test" name="testing" theme='simple' /]
[/@ww.param]
    [#if actionErrors?? && (actionErrors.size()>0)]
    <div class="warningBox">
        [#foreach error in formattedActionErrors]
                            ${error}
                    [/#foreach]
    </div>
    [/#if]

[@ww.hidden name='serverId'/]
[@ww.textfield labelKey="artifactory.server.url" name="url" required="true"/]
[@ww.textfield labelKey='artifactory.server.username' name="username"/]
[@ww.password labelKey='artifactory.server.password' name="password"/]
[@ww.textfield labelKey='artifactory.server.timeout' name="timeout" required="true"/]

[/@ww.form]
[#if mode=='edit']
</body></html>
[/#if]