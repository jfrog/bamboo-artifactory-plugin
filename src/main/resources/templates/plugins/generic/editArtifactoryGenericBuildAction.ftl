<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>

[@ui.bambooSection titleKey='artifactory.task.generic.deploy.title']

    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' emptyOption=true toggle='true'/]
<div id="genericArtifactoryConfigDiv">

    [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.deployerUsername'/]

    [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.deployerPassword' showPassword='true'/]

    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.radio labelKey='Upload by' name='artifactory.generic.useSpecsChoice' listKey='key' listValue='value' toggle='true' list=useSpecsOptions toggle='true'/]
    [@ui.bambooSection dependsOn='artifactory.generic.useSpecsChoice' showOn='specs']
        [@ww.select labelKey='artifactory.task.generic.deployPatternFileSpec' name='artifactory.generic.specSourceChoice' listKey='key' listValue='value' toggle='true' list=specSourceOptions/]
        [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='jobConfiguration']
            [@ww.textarea name='artifactory.generic.jobConfiguration' labelKey='artifactory.task.generic.deployPatternFileSpec.jobConfiguration' rows='10' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]
        [@ui.bambooSection dependsOn='artifactory.generic.specSourceChoice' showOn='file']
            [@ww.textarea name='artifactory.generic.file' labelKey='artifactory.task.generic.deployPatternFileSpec.file' rows='1' cols='80' cssClass="long-field" /]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
    [@ui.bambooSection dependsOn='artifactory.generic.useSpecsChoice' showOn='legacyPatterns']
            [@ww.textfield name='builder.artifactoryGenericBuilder.deployableRepo' labelKey='artifactory.task.maven.targetRepo'/]
            [@ww.textarea name='artifactory.generic.deployPattern' labelKey='artifactory.task.generic.deployPattern' rows='10' cols='80' cssClass="long-field"/]
    [/@ui.bambooSection]

    [@ww.checkbox name='buildInfoAggregation' toggle='true' cssStyle='visibility:hidden; position: absolute'/]
    [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=true]
        [@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]
        [@ui.bambooSection dependsOn='captureBuildInfo' id="captureBuildInfoSet" showOn=true]
            [#include 'editBuildNameNumberSnippet.ftl'/]
            [#include 'editEnvSnippet.ftl'/]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
    [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=false]
        [@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='artifactory.generic.publishBuildInfo' toggle='true'/]
        [@ui.bambooSection dependsOn='artifactory.generic.publishBuildInfo' id="publishBuildInfoSet"  showOn=true]
            [#include 'editBuildNameNumberSnippet.ftl'/]
            [#include 'editEnvSnippet.ftl'/]
        [/@ui.bambooSection]

    [/@ui.bambooSection]
</div>
[/@ui.bambooSection]

<script>
    var errorDiv = document.getElementById('artifactory-error');

    function clearError() {
        errorDiv.innerHTML = '';
        errorDiv.style.display = 'none';
    }

    function arrangeRadioButtons() {
        var useSpecsDiv = document.getElementById('artifactory_generic_useSpecsChoicespecs').parentNode;
        var useSpecslegacyPatternDiv = document.getElementById('artifactory_generic_useSpecsChoicelegacyPatterns').parentNode;
        useSpecsDiv.style.float = useSpecslegacyPatternDiv.style.float = 'left';
        useSpecsDiv.style.padding = useSpecslegacyPatternDiv.style.padding = '5px 0 0 20px';
        useSpecsDiv.style.margin = useSpecslegacyPatternDiv.style.margin = '0 0 0 20px';
    }

    clearError();
    arrangeRadioButtons();
    displayRequiredFieldset();
    function displayRequiredFieldset() {
        if (document.getElementsByName("buildInfoAggregation").length > 0 && document.getElementsByName("buildInfoAggregation")[0].checked) {
            // This is a new task, need to remove all fieldset that depends on old task.
            document.getElementById("publishBuildInfoSet").remove();
        } else {
            // This is an old task. Remove all fieldset that depends on the new task.
            document.getElementById("captureBuildInfoSet").remove();
        }
    }
</script>