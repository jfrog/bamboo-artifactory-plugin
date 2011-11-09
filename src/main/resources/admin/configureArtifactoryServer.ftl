[#if mode == 'edit' ]
    [#assign targetAction = 'updateServer']
    [#assign titleText = 'Edit Server Configuration' /]
<html>
<head><title>Update Artifactory Server</title></head>
<body>
    [#else]
        [#assign targetAction = 'createServer']
        [#assign titleText = 'Add New Server Configuration' /]
[/#if]
[#assign cancelUri = '/admin/manageArtifactoryServers.action' /]

<h1>[@ww.text name=titleText /]</h1>
<div class="paddedClearer"></div>
[@ww.form action=targetAction submitLabelKey='global.buttons.update'
titleKey='Artifactory Server Details'
cancelUri=cancelUri
descriptionKey='Configure an Artifactory server that will be available to project configurations for deployment of artifacts and build info.
If anonymous user is enabled in Artifactory server, you can leave the username/password empty'
showActionErrors='false']
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

[@ww.textfield labelKey='Artifactory Server URL' name="url" required="true"
descriptionKey="Specify the root URL of your Artifactory installation, like http://repo.jfrog.org/artifactory"/]

[@ww.textfield labelKey='Username' name="username"
descriptionKey="User with permissions to query the list of Artifactory repositories. Leave empty if anonymous is enabled."/]

[@ww.password labelKey='Password' name="password" showPassword="true"
descriptionKey="Password of the user with permissions to query the list of Artifactory repositories. Leave empty if anonymous is enabled."/]

[@ww.textfield labelKey='Request Timeout' name="timeout" required="true"
descriptionKey="Network timeout in seconds to use both for connection establishment and for unanswered requests."/]

[/@ww.form]
[#if mode=='edit']
</body></html>
[/#if]