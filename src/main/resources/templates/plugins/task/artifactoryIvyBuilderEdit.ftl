[@ww.textfield labelKey='Build File' name='builder.artifactoryIvyBuilder.buildFile' required='true' /]
[@ww.textfield labelKey='Targets' name='builder.artifactoryIvyBuilder.target' required='true'/]

[#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
[@ww.select labelKey='builder.common.jdk' name='builder.artifactoryIvyBuilder.buildJdk' cssClass="jdkSelectWidget"
list=uiConfigBean.jdkLabels required='true'
extraUtility=addJdkLink /]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='ivy'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryIvyBuilder.executable'
list=uiConfigBean.getExecutableLabels('ivy') extraUtility=addExecutableLink required='true' /]
[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryIvyBuilder.environmentVariables'
descriptionKey='Space-separated key-value pairs of extra environment variables to pass to the build process (e.g. EXT_PATH=/var/lib/ext).' /]
[@ww.textfield labelKey='Ant Opts' name='builder.artifactoryIvyBuilder.antOpts' descriptionKey='Space-separated parameters to pass as ANT_OPTS (Note that ANT_OPTS will be ignored if added as System Environment Variables).'/]
[@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryIvyBuilder.workingSubDirectory' helpUri='working-directory.ftl' /]

[@ww.select name='builder.artifactoryIvyBuilder.artifactoryServerId' labelKey='Artifactory Server URL' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayIvyArtifactoryConfigs(this.value)' emptyOption=true toggle='true'
descriptionKey='Select an Artifactory server.'/]

<div id="ivyArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryIvyBuilder.deployableRepo' labelKey='Target Repository' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey='Select a target deployment repository.'/]

[@ww.textfield labelKey='Deployer Username' name='builder.artifactoryIvyBuilder.deployerUsername'
descriptionKey='Name of a user with deployment permissions on the target repository.'/]

[@ww.password labelKey='Deployer Password' name='builder.artifactoryIvyBuilder.deployerPassword' showPassword='true'
descriptionKey='The password of the user entered above.'/]

[@ww.checkbox labelKey='Deploy Artifacts' name='deployArtifacts' toggle='true'
descriptionKey='Uncheck if you do not wish to deploy artifacts from the plugin.'/]

[@ui.bambooSection dependsOn='deployArtifacts' showOn=true]
    [@ww.textfield labelKey='Deployment Include Patterns'
    name='builder.artifactoryIvyBuilder.deployIncludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be included in publishing. Include patterns are applied on the published file path before any
    exclude patterns.'/]
    [@ww.textfield labelKey='Deployment Exclude Patterns'
    name='builder.artifactoryIvyBuilder.deployExcludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be excluded from publishing. Exclude patterns are applied on the published file path before any
    exclude patterns.'/]

    [@ww.checkbox labelKey='Use Maven 2 Compatible Patterns' name='useM2CompatiblePatterns' toggle='true'
    descriptionKey='Whether to use the default Maven 2 patterns when publishing artifacts and Ivy descriptors, or to use custom patterns.'/]

    [@ui.bambooSection dependsOn='useM2CompatiblePatterns' showOn='false']
        [@ww.textfield labelKey='Ivy Pattern' name='builder.artifactoryIvyBuilder.ivyPattern'
        descriptionKey='The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a> to use for published Ivy descriptors.'/]
        [@ww.textfield labelKey='Artifact Pattern' name='builder.artifactoryIvyBuilder.artifactPattern'
        descriptionKey='The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a> to use for published artifacts.'/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ww.checkbox labelKey='Capture and Publish Build Info' name='publishBuildInfo'
toggle='true' descriptionKey='Check if you wish to publish build information to Artifactory.'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='Include Environment Variables' name='includeEnvVars'
    toggle='true' descriptionKey='Check if you wish to include all environment variables accessible by the builds process.'/]

    [@ui.bambooSection dependsOn='includeEnvVars' showOn=true]
        [@ww.textfield labelKey='Environment Variables Include Patterns'
        name='envVarsIncludePatterns'
        descriptionKey='Comma or space-separated list of environment variables that will be included as part of the published build info.
        Environment variables may contain the * and the ? wildcards. Include patterns are applied before any exclude patterns.'/]
        [@ww.textfield labelKey='Environment Variables Exclude Patterns'
        name='envVarsExcludePatterns'
        descriptionKey='Comma or space-separated list of environment variables that will be excluded as part of the published build info.
        Environment variables may contain the * and the ? wildcards. Exclude patterns are applied after any include patterns.'/]
    [/@ui.bambooSection]

    [@ww.checkbox labelKey='Run License Checks (Requires Pro)' name='runLicenseChecks'
    toggle='true' descriptionKey='Check if you wish that automatic license scanning will occur after build is complete.'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='Send License Violation Notifications To'
        name='builder.artifactoryIvyBuilder.licenseViolationRecipients' descriptionKey='Whitespace-separated list of recipient addresses.'/]

        [@ww.textfield labelKey='Limit Checks To The Following Scopes'
        name='builder.artifactoryIvyBuilder.limitChecksToScopes' descriptionKey='Space-separated list of scopes.'/]

        [@ww.checkbox labelKey='Include Published Artifacts' name='builder.artifactoryIvyBuilder.includePublishedArtifacts'
        toggle='true' descriptionKey="Include the build's published module artifacts in the license violation checks if they are also used
                    as dependencies for other modules in this build."/]

        [@ww.checkbox labelKey='Disable Automatic License Discovery' name='builder.artifactoryIvyBuilder.disableAutoLicenseDiscovery'
        toggle='true' descriptionKey="Tells Artifactory to not try and automatically analyze and tag the build's dependencies with license information
                    upon deployment. You can still attach license information manually by running 'Auto-Find' from the build's
                    Licenses tab in Artifactory."/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ui.bambooSection titleKey='builder.common.tests.directory.description']
    [@ww.checkbox labelKey='builder.common.tests.exists' name='testChecked' toggle='true'/]

    [@ui.bambooSection dependsOn='testChecked' showOn=true]
        [@ww.radio labelKey='builder.common.tests.directory' name='testDirectoryOption'
        listKey='key' listValue='value' toggle='true'
        list=testDirectoryTypes ]
        [/@ww.radio]
        [@ui.bambooSection dependsOn='testDirectoryOption' showOn='customTestDirectory']
            [@ww.textfield labelKey='builder.common.tests.directory.custom' name='builder.artifactoryIvyBuilder.testResultsDirectory' /]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
[/@ui.bambooSection]
</div>

<script>
    function displayIvyArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('ivyArtifactoryConfigDiv');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('builder.artifactoryIvyBuilder.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }

            loadIvyRepoKeys(serverId)
        }
    }

    function loadIvyRepoKeys(serverId) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true',
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document.getElementsByName('builder.artifactoryIvyBuilder.deployableRepo')[0];
                repoSelect.innerHTML = '';
                if (serverId >= 0) {

                    var selectedRepoKey = '${selectedRepoKey}';

                    for (var i = 0, l = json.length; i < l; i++) {
                        var deployableRepoKey = json[i];
                        var option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        repoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey == selectedRepoKey)) {
                            repoSelect.selectedIndex = i;
                        }
                    }
                }
            },
            error:function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the target repository list.\n' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.\n';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accesible.'
                } else {
                    errorMessage +=
                            'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }
                alert(errorMessage);
            }
        });
    }
    displayIvyArtifactoryConfigs(${selectedServerId});
</script>
