[@ui.bambooSection titleKey='artifactory.task.deploy.title']
    [@ww.select name='artifactory.deployment.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayDeployArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
<div id="deployArtifactoryConfiguration">
    [@ww.textfield name='artifactory.deployment.username' labelKey='artifactory.task.maven.deployerUsername'/]

    [@ww.password name='artifactory.deployment.password' labelKey='artifactory.task.maven.deployerPassword' showPassword='true'/]
    [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

    [@ww.select labelKey='artifactory.task.generic.resolvePatternFileSpec' name='artifactory.deployment.specSourceChoice' listKey='key' listValue='value' toggle='true' list=specSourceOptions/]
    [@ui.bambooSection dependsOn='artifactory.deployment.specSourceChoice' showOn='jobConfiguration']
        [@ww.textarea labelKey='artifactory.task.generic.deployPatternFileSpec.jobConfiguration' name='artifactory.deployment.jobConfiguration' rows='10' cols='80' cssClass="long-field" /]
    [/@ui.bambooSection]
    [@ui.bambooSection dependsOn='artifactory.deployment.specSourceChoice' showOn='file']
        [@ww.textarea labelKey='artifactory.task.generic.deployPatternFileSpec.file' name='artifactory.deployment.file'  rows='1' cols='80' cssClass="long-field" /]
    [/@ui.bambooSection]

</div>
[/@ui.bambooSection]

<script>
    function displayDeployArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('deployArtifactoryConfiguration');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('artifactory.deployment.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }

        }
    }

    displayDeployArtifactoryConfigs(${selectedServerId});
</script>