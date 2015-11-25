<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>
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
[@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryIvyBuilder.workingSubDirectory' helpUri='working-directory.ftl' /]

[@ww.select name='builder.artifactoryIvyBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayIvyArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]

<div id="ivyArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryIvyBuilder.deployableRepo' labelKey='artifactory.task.maven.targetRepo' list=dummyList listKey='repoKey' listValue='repoKey' toggle='true'/]

[@ww.password name='builder.artifactoryIvyBuilder.deployerUsername.DUMMY' cssStyle='display: none;'/]
[@ww.textfield labelKey='artifactory.task.maven.deployerUsername' name='builder.artifactoryIvyBuilder.deployerUsername' /]

[@ww.password name='builder.artifactoryIvyBuilder.deployerPassword.DUMMY' cssStyle='display: none;'/]
[@ww.password labelKey='artifactory.task.maven.deployerPassword' name='builder.artifactoryIvyBuilder.deployerPassword' showPassword='true'/]

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

[@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='publishBuildInfo' toggle='true'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='includeEnvVars' toggle='true'/]

    [@ui.bambooSection dependsOn='includeEnvVars' showOn=true]
        [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='envVarsIncludePatterns' /]
        [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='envVarsExcludePatterns' /]
    [/@ui.bambooSection]

    [@ww.checkbox labelKey='artifactory.task.runLicenseChecks' name='runLicenseChecks' toggle='true'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryIvyBuilder.licenseViolationRecipients'/]

        [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryIvyBuilder.limitChecksToScopes'/]

        [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryIvyBuilder.includePublishedArtifacts' toggle='true'/]

        [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryIvyBuilder.disableAutoLicenseDiscovery' toggle='true'/]
    [/@ui.bambooSection]

    [#--blackduck integration--]
    [#include 'BlackDuckBuilderEditSnippet.ftl'/]
[/@ui.bambooSection]

[@ww.checkbox labelKey="Bintray configuration" name="bintrayConfiguration" toggle='true'/]

[@ui.bambooSection dependsOn="bintrayConfiguration"  showOn=true]
    [@ww.textfield name="bintray.subject" labelKey="artifactory.task.pushToBintray.subject"/]
    [@ww.textfield name="bintray.repository" labelKey="artifactory.task.pushToBintray.repository"/]
    [@ww.textfield name="bintray.packageName" labelKey="artifactory.task.pushToBintray.packageName"/]
    [@ww.textfield name="bintray.licenses" labelKey="artifactory.task.pushToBintray.licenses"/]
    [@ww.textfield name="bintray.vcsUrl" labelKey="artifactory.task.pushToBintray.vcsUrl"/]
    [@ww.select name="bintray.signMethod" label="Sign method" list=signMethods listKey='key' listValue='value'/]
    [@ww.textfield name="bintray.gpgPassphrase" labelKey= "GPG Passphrase"/]
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
        var credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[4].value;

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

            loadIvyRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadIvyRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
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
                var errorMessage = 'An error has occurred while retrieving the target repository list.<br>' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accessible.'
                } else {
                    errorMessage +=
                            'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }

                errorMessage += "<br>";
                errorDiv.innerHTML += errorMessage;
                errorDiv.style.display = '';
            }
        });
    }
    var errorDiv = document.getElementById('artifactory-error');
    errorDiv.innerHTML = '';
    errorDiv.style.display = 'none';
    displayIvyArtifactoryConfigs(${selectedServerId});
</script>
