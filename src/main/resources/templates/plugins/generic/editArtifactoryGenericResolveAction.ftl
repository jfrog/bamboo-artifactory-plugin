[@ui.bambooSection titleKey='artifactory.task.generic.resolve.title']

    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]
<div id="genericArtifactoryConfigDiv">

    [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.resolverUsername'/]

    [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.resolverPassword' showPassword='true'/]
    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.radio labelKey='Download by' name='artifactory.generic.useSpecsChoice' listKey='key' listValue='value' toggle='true' list=useSpecsOptions toggle='true'/]
    [@ui.bambooSection dependsOn='artifactory.generic.useSpecsChoice' showOn='specs']
    [@ww.select labelKey='artifactory.task.generic.resolvePatternFileSpec' name='artifactory.generic.specSourceChoice' listKey='key' listValue='value' toggle='true' list=specSourceOptions/]
    [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='jobConfiguration']
        [@ww.textarea name='artifactory.generic.jobConfiguration' labelKey='artifactory.task.generic.resolvePatternFileSpec.jobConfiguration' rows='10' cols='80' cssClass="long-field" /]
    [/@ui.bambooSection]
    [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='file']
        [@ww.textarea name='artifactory.generic.file' labelKey='artifactory.task.generic.resolvePatternFileSpec.file' rows='1' cols='80' cssClass="long-field" /]
    [/@ui.bambooSection]
    [/@ui.bambooSection]
        [@ui.bambooSection dependsOn='artifactory.generic.useSpecsChoice' showOn='legacyPatterns']
        [@ww.textarea name='artifactory.generic.resolvePattern' labelKey='artifactory.task.generic.resolvePattern' rows='10' cols='80' cssClass="long-field" /]
    [/@ui.bambooSection]
    [@ww.checkbox name='buildInfoAggregation' toggle='true' cssStyle='visibility:hidden; position: absolute'/]
    [@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]

    [@ui.bambooSection dependsOn='captureBuildInfo' showOn=true]
        [#include '../task/editBuildNameNumberSnippet.ftl'/]
        [#include 'editEnvSnippet.ftl'/]
    [/@ui.bambooSection]

</div>
[/@ui.bambooSection]

<script>
    var useSpecsDiv = document.getElementById('artifactory_generic_useSpecsChoicespecs').parentNode;
    var useSpecslegacyPatternDiv = document.getElementById('artifactory_generic_useSpecsChoicelegacyPatterns').parentNode;
    useSpecsDiv.style.float = useSpecslegacyPatternDiv.style.float = 'left';
    useSpecsDiv.style.padding = useSpecslegacyPatternDiv.style.padding = '5px 0 0 20px';
    useSpecsDiv.style.margin = useSpecslegacyPatternDiv.style.margin = '0 0 0 20px';
</script>
