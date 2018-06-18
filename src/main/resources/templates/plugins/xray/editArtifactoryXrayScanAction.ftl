<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>

[@ui.bambooSection titleKey='artifactory.task.xrayScan.title']

    [@ww.select name='artifactory.xrayScan.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]

<div id="xrayScanConfigDiv">

    [@ww.textfield name='artifactory.xrayScan.username' labelKey='artifactory.task.xrayScan.username'/]

    [@ww.password name='artifactory.xrayScan.password' labelKey='artifactory.task.xrayScan.password' showPassword='true'/]

[#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.checkbox name='artifactory.xrayScan.failIfVulnerable' labelKey='artifactory.task.xrayScan.failIfVulnerable' toggle='true' /]

</div>
[/@ui.bambooSection]
