[@ui.bambooSection titleKey='artifactory.task.collectBuildIssues.title']

    [@ww.select name='artifactory.task.collectBuildIssues.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]
<div id="collectBuildIssuesArtifactoryConfigDiv">

    [@ww.textfield name='artifactory.task.collectBuildIssues.username' labelKey='artifactory.task.collectBuildIssues.header.username'/]

    [@ww.password name='artifactory.task.collectBuildIssues.password' labelKey='artifactory.task.collectBuildIssues.header.password' showPassword='true'/]
    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.select labelKey='artifactory.task.collectBuildIssues.header.config.source' name='artifactory.task.collectBuildIssues.config.source' listKey='key' listValue='value' toggle='true' list=configSourceOptions/]
        [@ui.bambooSection dependsOn='artifactory.task.collectBuildIssues.config.source' showOn='taskConfiguration']
            [@ww.textarea name='artifactory.task.collectBuildIssues.config.source.taskConfiguration' labelKey='artifactory.task.collectBuildIssues.header.config.json' rows='10' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]
        [@ui.bambooSection dependsOn='artifactory.task.collectBuildIssues.config.source' showOn='file']
            [@ww.textarea name='artifactory.task.collectBuildIssues.config.source.file' labelKey='artifactory.task.collectBuildIssues.header.config.filePath' rows='1' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]

</div>
[/@ui.bambooSection]
