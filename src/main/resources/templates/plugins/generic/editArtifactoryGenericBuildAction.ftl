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

    [@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='artifactory.generic.publishBuildInfo' toggle='true' /]

    [@ui.bambooSection dependsOn='artifactory.generic.publishBuildInfo' showOn=true]

        [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='artifactory.generic.includeEnvVars' toggle='true'/]

        [@ui.bambooSection dependsOn='artifactory.generic.includeEnvVars' showOn=true]
            [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='artifactory.generic.envVarsIncludePatterns' /]
            [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='artifactory.generic.envVarsExcludePatterns' /]
        [/@ui.bambooSection]

        [@ww.checkbox labelKey="Bintray configuration (deprecated)" name="bintrayConfiguration" toggle='true'/]
        [@ui.bambooSection dependsOn="bintrayConfiguration"  showOn=true]
            [@ww.textfield name="bintray.subject" labelKey="artifactory.task.pushToBintray.subject"/]
            [@ww.textfield name="bintray.repository" labelKey="artifactory.task.pushToBintray.repository"/]
            [@ww.textfield name="bintray.packageName" labelKey="artifactory.task.pushToBintray.packageName"/]
            [@ww.textfield name="bintray.licenses" labelKey="artifactory.task.pushToBintray.licenses"/]
            [@ww.textfield name="bintray.vcsUrl" labelKey="artifactory.task.pushToBintray.vcsUrl"/]
            [@ww.select name="bintray.signMethod" label="Sign method" list=signMethods listKey='key' listValue='value'/]
            [@ww.textfield name="bintray.gpgPassphrase" labelKey= "GPG Passphrase"/]
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
</script>