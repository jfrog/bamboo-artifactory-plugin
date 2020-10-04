[#--Build--]
[@ui.bambooSection titleKey='artifactory.task.buildConfigurationsTitle' collapsible=true]
    [@ww.textfield labelKey='artifactory.task.ivy.buildFile' name='builder.artifactoryIvyBuilder.buildFile' required='true' /]
    [@ww.textfield labelKey='artifactory.task.ivy.targets' name='builder.artifactoryIvyBuilder.target' required='true'/]

    <div id="buildJdkSelectionDiv">
        [#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
        [@ww.select labelKey='builder.common.jdk' name='builder.artifactoryIvyBuilder.buildJdk' cssClass="jdkSelectWidget"
        list=uiConfigBean.jdkLabels required='true'
        extraUtility=addJdkLink /]
    </div>
    <div id="buildJdkOverridenDiv">
    </div>

    [#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='ivy'/][/#assign]
    [@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryIvyBuilder.executable'
    list=uiConfigBean.getExecutableLabels('ivy') extraUtility=addExecutableLink required='true' /]
    [@ww.textfield labelKey='builder.common.env' name='builder.artifactoryIvyBuilder.environmentVariables' /]
    [@ww.textfield labelKey='artifactory.task.ivy.antOpts' name='builder.artifactoryIvyBuilder.antOpts'/]
    [@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryIvyBuilder.workingSubDirectory' /]

    [@ui.bambooSection titleKey='builder.common.tests.directory.description']
        [@ww.checkbox labelKey='builder.common.tests.exists' name='testChecked' toggle='true'/]
        [@ui.bambooSection dependsOn='testChecked' showOn=true]
            [@ww.radio labelKey='builder.common.tests.directory' name='testDirectoryOption'
            listKey='key' listValue='value' toggle='true'
            list=testDirectoryTypes /]
            [@ui.bambooSection dependsOn='testDirectoryOption' showOn='customTestDirectory']
                [@ww.textfield labelKey='builder.common.tests.directory.custom' name='builder.artifactoryIvyBuilder.testResultsDirectory' /]
            [/@ui.bambooSection]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[#--Deployment--]
[@ui.bambooSection id="deploymentSection" titleKey='artifactory.task.deploymentConfigurationsTitle' collapsible=true]
    [@ww.select name='builder.artifactoryIvyBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayIvyArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]

    <div id="ivyArtifactoryConfigDiv">
        [@ww.select name='builder.artifactoryIvyBuilder.deployableRepo' labelKey='artifactory.task.maven.targetRepo' list=dummyList listKey='repoKey' listValue='repoKey' toggle='true'/]
        <div id="deploy-repo-error" class="aui-message aui-message-error error shadowed"
             style="display: none; width: 80%; font-size: 80%"/>

        [@ww.select labelKey='Override credentials' name='deployer.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield labelKey='artifactory.task.maven.deployerUsername' name='builder.artifactoryIvyBuilder.deployerUsername' onchange='javascript: overridingCredentialsChanged()' /]
            [@ww.password labelKey='artifactory.task.maven.deployerPassword' name='builder.artifactoryIvyBuilder.deployerPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged()' /]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='deployer.sharedCredentials' labelKey='artifactory.task.generic.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]

        [@ww.checkbox labelKey='artifactory.task.ivy.deployArtifacts' name='deployArtifacts' toggle='true'/]
        [@ui.bambooSection dependsOn='deployArtifacts' showOn=true]
            [@ww.textfield labelKey='artifactory.task.deployIncludePatterns' name='builder.artifactoryIvyBuilder.deployIncludePatterns'/]
            [@ww.textfield labelKey='artifactory.task.deployExcludePatterns' name='builder.artifactoryIvyBuilder.deployExcludePatterns' /]
            [@ww.checkbox labelKey='artifactory.task.filterExcludedArtifactsFromBuild' name='builder.artifactoryIvyBuilder.filterExcludedArtifactsFromBuild' toggle="true"/]

            [@ww.checkbox labelKey='artifactory.task.gradle.useM2CompatiblePatterns' name='useM2CompatiblePatterns' toggle='true'/]
            [@ui.bambooSection dependsOn='useM2CompatiblePatterns' showOn='false']
                [@ww.textfield labelKey='artifactory.task.gradle.ivyPattern' name='builder.artifactoryIvyBuilder.ivyPattern'/]
                [@ww.textfield labelKey='artifactory.task.gradle.artifactPattern' name='builder.artifactoryIvyBuilder.artifactPattern'/]
            [/@ui.bambooSection]
        [/@ui.bambooSection]

        [#--Build Info and Release Management--]
        [@ui.bambooSection titleKey='artifactory.task.buildInfoTitle' collapsible=true]
            [@ww.checkbox name='buildInfoAggregation' toggle='true' cssStyle='visibility:hidden; position: absolute'/]
            [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=true]
                [@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]
                [@ui.bambooSection dependsOn='captureBuildInfo' id="captureBuildInfoSet" showOn=true]
                    [#include 'editEnvVarsSnippet.ftl'/]
                [/@ui.bambooSection]
            [/@ui.bambooSection]
            [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=false]
                [@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='publishBuildInfo' toggle='true'/]
                [@ui.bambooSection dependsOn='publishBuildInfo' id="publishBuildInfoSet" showOn=true]
                    [#include 'editEnvVarsSnippet.ftl'/]
                [/@ui.bambooSection]
            [/@ui.bambooSection]
        [/@ui.bambooSection]

    </div>
[/@ui.bambooSection]

<script>

    function displayIvyArtifactoryConfigs(serverId) {
        let configDiv = document.getElementById('ivyArtifactoryConfigDiv');
        let credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        let credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        if ((serverId == null) || (serverId.length === 0) || (-1 === serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            let urlSelect = document.getElementsByName('builder.artifactoryIvyBuilder.artifactoryServerId')[0];
            let urlOptions = urlSelect.options;
            for (let i = 0; i < urlOptions.length; i++) {
                let option = urlOptions[i];
                if (option.value === '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadIvyRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadIvyRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                repoSelect.innerHTML = '';
                if (serverId >= 0) {
                    let selectedRepoKey = '${selectedRepoKey}';
                    for (let i = 0, l = json.length; i < l; i++) {
                        let deployableRepoKey = json[i];
                        let option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        repoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey === selectedRepoKey)) {
                            repoSelect.selectedIndex = i;
                        }
                    }
                }
                errorDiv.innerHTML = '';
                errorDiv.style.display = 'none';
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                let errorMessage = 'An error has occurred while retrieving the target repository list.<br>' +
                    'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status === 404) {
                    errorMessage +=
                        'Please make sure that the Artifactory Server Configuration Management Servlet is accessible.'
                } else {
                    errorMessage +=
                        'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }
                errorMessage += "<br>";
                errorDiv.innerHTML = errorMessage;
                errorDiv.style.display = '';
                repoSelect.innerHTML = '';
            }
        });
    }

    function overridingCredentialsChanged() {
        let serverIdSection = document.getElementById("deploymentSection");
        let serverId = serverIdSection.getElementsByTagName('select')[0].value;
        displayIvyArtifactoryConfigs(serverId);
    }

    function displayRequiredFieldset() {
        if (document.getElementsByName("buildInfoAggregation").length > 0 && document.getElementsByName("buildInfoAggregation")[0].checked) {
            // This is a new task, need to remove all fieldset that depends on old task.
            document.getElementById("publishBuildInfoSet").remove();
        } else {
            // This is an old task. Remove all fieldset that depends on the new task.
            document.getElementById("captureBuildInfoSet").remove();
        }
    }

    let errorDiv = document.getElementById('deploy-repo-error');
    errorDiv.innerHTML = '';
    errorDiv.style.display = 'none';
    let repoSelect = document.getElementsByName('builder.artifactoryIvyBuilder.deployableRepo')[0];
    displayIvyArtifactoryConfigs(${selectedServerId});
    displayRequiredFieldset();

</script>
