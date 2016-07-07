[@ui.bambooSection titleKey='artifactory.task.generic.resolve.title']
    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayGenericArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
<div id="genericArtifactoryConfigDiv">

    [#--The Dummy tags are workaround for the autocomplete (Chorme)--]
    [@ww.password name='artifactory.generic.username.DUMMY' cssStyle='display: none;'/]
    [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.resolverUsername'/]

    [@ww.password name='artifactory.generic.password.DUMMY' cssStyle='display: none;'/]
    [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.resolverPassword' showPassword='true'/]

    [@ww.textarea name='artifactory.generic.resolvePattern' labelKey='artifactory.task.generic.resolvePattern' rows='10' cols='80' cssClass="long-field" /]

</div>
[/@ui.bambooSection]

