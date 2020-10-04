<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>

[@ui.bambooSection titleKey='artifactory.task.xrayScan.title']

    [@ww.select name='artifactory.xrayScan.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true' required='true'/]

    <div id="xrayScanConfigDiv">

        [@ww.select labelKey='Override credentials' name='deployer.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield name='artifactory.xrayScan.username' labelKey='artifactory.task.xrayScan.username'/]
            [@ww.password name='artifactory.xrayScan.password' labelKey='artifactory.task.xrayScan.password' showPassword='true'/]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='deployer.sharedCredentials' labelKey='artifactory.task.generic.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]

        [@ww.checkbox name='artifactory.xrayScan.failIfVulnerable' labelKey='artifactory.task.xrayScan.failIfVulnerable' toggle='true' /]

    </div>
[/@ui.bambooSection]
