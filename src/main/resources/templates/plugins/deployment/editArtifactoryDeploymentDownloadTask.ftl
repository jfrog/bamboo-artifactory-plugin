[@ui.bambooSection titleKey='artifactory.task.deploy.download.title']

    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]
    <div id="genericArtifactoryConfigDiv">

        [@ww.select labelKey='Override credentials' name='resolver.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.resolverUsername'/]
            [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.resolverPassword' showPassword='true'/]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='resolver.sharedCredentials' labelKey='artifactory.task.generic.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]

        [@ww.select labelKey='artifactory.task.generic.resolvePatternFileSpec' name='artifactory.generic.specSourceChoice' listKey='key' listValue='value' toggle='true' list=specSourceOptions/]
        [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='jobConfiguration']
            [@ww.textarea name='artifactory.generic.jobConfiguration' labelKey='artifactory.task.generic.resolvePatternFileSpec.jobConfiguration' rows='10' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]
        [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='file']
            [@ww.textarea name='artifactory.generic.file' labelKey='artifactory.task.generic.resolvePatternFileSpec.file' rows='1' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]

    </div>
[/@ui.bambooSection]