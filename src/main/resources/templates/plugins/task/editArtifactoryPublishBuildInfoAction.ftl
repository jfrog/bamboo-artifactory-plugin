<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>

[@ui.bambooSection titleKey='artifactory.task.publishBuildInfo.title']

    [@ww.select name='artifactory.task.publishBuildInfo.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true' required='true'/]

<div id="xrayScanConfigDiv">

    [@ww.textfield name='artifactory.task.publishBuildInfo.username' labelKey='artifactory.task.publishBuildInfo.header.username'/]

    [@ww.password name='artifactory.task.publishBuildInfo.password' labelKey='artifactory.task.publishbuildinfo.header.password' showPassword='true'/]

[#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

</div>
[/@ui.bambooSection]
