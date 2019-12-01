[@ui.bambooSection titleKey='artifactory.task.collectIssues.title']

    [@ww.select name='artifactory.task.collectIssues.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]
<div id="collectIssuesArtifactoryConfigDiv">

    [@ww.textfield name='artifactory.task.collectIssues.username' labelKey='artifactory.task.collectIssues.header.username'/]

    [@ww.password name='artifactory.task.collectIssues.password' labelKey='artifactory.task.collectIssues.header.password' showPassword='true'/]
    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.select labelKey='Config Source' name='artifactory.task.collectIssues.configSourceChoice' listKey='key' listValue='value' toggle='true' list=configSourceOptions/]
        [@ui.bambooSection dependsOn='artifactory.task.collectIssues.configSourceChoice' showOn='taskConfiguration']
            [@ww.textarea name='artifactory.task.collectIssues.configSource.taskConfiguration' labelKey='artifactory.task.collectIssues.configJson' rows='10' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]
        [@ui.bambooSection dependsOn='artifactory.task.collectIssues.configSourceChoice' showOn='file']
            [@ww.textarea name='artifactory.task.collectIssues.configSource.file' labelKey='artifactory.task.collectIssues.configFilePath' rows='1' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]

</div>
[/@ui.bambooSection]
