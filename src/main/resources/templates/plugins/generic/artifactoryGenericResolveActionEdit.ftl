[@ui.bambooSection titleKey='artifactory.task.generic.resolve.title']

    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayGenericArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
<div id="genericArtifactoryConfigDiv">

    [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.resolverUsername'/]

    [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.resolverPassword' showPassword='true'/]
    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.textarea name='artifactory.generic.resolvePattern' labelKey='artifactory.task.generic.resolvePattern' rows='10' cols='80' cssClass="long-field" /]

</div>
[/@ui.bambooSection]

