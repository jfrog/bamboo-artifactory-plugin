[#if mode == 'edit' ]
    [#assign targetAction = 'updateServer']
    [#assign titleText = 'artifactory.server.edit' /]
<html>
<head><title>Update Artifactory Server</title></head>
<body>
[#else]
    [#assign targetAction = 'createServer']
[/#if]

[#assign cancelUri = '/admin/manageArtifactoryServers.action' /]

<div class="paddedClearer"></div>
[@ww.form action=targetAction submitLabelKey='global.buttons.update'
titleKey='artifactory.server.details'
cancelUri=cancelUri
descriptionKey='artifactory.server.description'
showActionErrors='true']

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
    [@ww.password labelKey='artifactory.server.password' name="password" showPassword='true'/]
    [@ww.textfield labelKey='artifactory.server.timeout' name="timeout" required="true"/]
<button class="button" id="createServer_testing" type="submit" value="Test" name="testing">Test Connection</button>
<h1>Bintray Configuration</h1>
    [@ww.textfield labelKey="Bintray Username" name="bintrayUsername"/]
    [@ww.password labelKey="Bintray Api Key" name="bintrayApiKey" showPassword='true' /]
    [@ww.textfield labelKey="Sonaytype OSS Username" name="sonatypeOssUsername" /]
    [@ww.password labelKey="Sonatype OSS Password" name="sonatypeOssPassword" showPassword='true' /]
[/@ww.form]

[#if mode=='edit']
</body>localh
[/#if]