[@ww.textfield labelKey='artifactory.task.gradle.switches' name='builder.artifactoryGradleBuilder.switches'/]

[@ww.textfield labelKey='artifactory.task.gradle.tasks' name='builder.artifactoryGradleBuilder.tasks'/]

[@ww.textfield labelKey='artifactory.task.gradle.buildScript' name='builder.artifactoryGradleBuilder.buildScript'/]

[@ww.textfield labelKey='artifactory.task.gradle.buildFile' name='builder.artifactoryGradleBuilder.buildFile'/]

[#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
[@ww.select labelKey='builder.common.jdk' name='builder.artifactoryGradleBuilder.buildJdk' cssClass="jdkSelectWidget"
list=uiConfigBean.jdkLabels required='true'
extraUtility=addJdkLink /]

[@ww.checkbox labelKey='artifactory.task.gradle.useGradleWrapper' name='builder.artifactoryGradleBuilder.useGradleWrapper' toggle='true'/]

[@ui.bambooSection dependsOn='builder.artifactoryGradleBuilder.useGradleWrapper' showOn=true]
    [@ww.textfield labelKey='artifactory.task.gradle.gradleWrapperLocation' name='builder.artifactoryGradleBuilder.gradleWrapperLocation'/]
[/@ui.bambooSection]

[@ui.bambooSection dependsOn='builder.artifactoryGradleBuilder.useGradleWrapper' showOn=false]
    [#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='gradle'/][/#assign]
    [@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryGradleBuilder.executable'
    list=uiConfigBean.getExecutableLabels('gradle') extraUtility=addExecutableLink/]
[/@ui.bambooSection]

[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryGradleBuilder.environmentVariables' /]

[@ww.select name='builder.artifactoryGradleBuilder.artifactoryServerId' labelKey='artifactory.task.gradle.artifactoryServerId' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayGradleArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]

<div id="gradleArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryGradleBuilder.resolutionRepo' labelKey='artifactory.task.gradle.resolutionRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true'/]

[@ww.select name='builder.artifactoryGradleBuilder.publishingRepo' labelKey='artifactory.task.gradle.publishingRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true'/]

[@ww.textfield labelKey='artifactory.task.gradle.deployerUsername' name='builder.artifactoryGradleBuilder.deployerUsername'/]

[@ww.password labelKey='artifactory.task.gradle.deployerPassword' name='builder.artifactoryGradleBuilder.deployerPassword' showPassword='true'/]

[@ww.checkbox labelKey='artifactory.task.gradle.useArtifactoryGradlePlugin' name='builder.artifactoryGradleBuilder.useArtifactoryGradlePlugin' toggle='true'/]

[@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='publishBuildInfo' toggle='true'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='includeEnvVars' toggle='true' /]

    [@ui.bambooSection dependsOn='includeEnvVars' showOn=true]
        [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='envVarsIncludePatterns'/]
        [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='envVarsExcludePatterns'/]
    [/@ui.bambooSection]

    [@ww.checkbox labelKey='artifactory.task.runLicenseChecks' name='runLicenseChecks' toggle='true'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryGradleBuilder.licenseViolationRecipients'/]

        [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryGradleBuilder.limitChecksToScopes'/]

        [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryGradleBuilder.includePublishedArtifacts' toggle='true'/]

        [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryGradleBuilder.disableAutoLicenseDiscovery' toggle='true'/]
    [/@ui.bambooSection]

    [#--blackduck integration--]
    [#include 'BlackDuckBuilderEditSnippet.ftl'/]

[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.gradle.publishArtifacts' name='publishArtifacts' toggle='true'/]

[@ui.bambooSection dependsOn='publishArtifacts' showOn=true]

    [@ww.checkbox labelKey='artifactory.task.gradle.publishMavenDescriptors' name='builder.artifactoryGradleBuilder.publishMavenDescriptors' toggle='true'/]

    [@ww.checkbox labelKey='artifactory.task.gradle.publishIvyDescriptors' name='builder.artifactoryGradleBuilder.publishIvyDescriptors' toggle='true'/]

    [@ww.checkbox labelKey='artifactory.task.gradle.useM2CompatiblePatterns' name='useM2CompatiblePatterns' toggle='true'/]

    [@ui.bambooSection dependsOn='useM2CompatiblePatterns' showOn='false']
        [@ww.textfield labelKey='artifactory.task.gradle.ivyPattern' name='builder.artifactoryGradleBuilder.ivyPattern'/]
        [@ww.textfield labelKey='artifactory.task.gradle.artifactPattern' name='builder.artifactoryGradleBuilder.artifactPattern'/]
    [/@ui.bambooSection]

    [@ww.textfield labelKey='artifactory.task.gradle.publishIncludePatterns' name='builder.artifactoryGradleBuilder.publishIncludePatterns'/]
    [@ww.textfield labelKey='artifactory.task.gradle.publishExcludePatterns' name='builder.artifactoryGradleBuilder.publishExcludePatterns'/]
    [@ww.checkbox labelKey='artifactory.task.filterExcludedArtifactsFromBuild' name='builder.artifactoryGradleBuilder.filterExcludedArtifactsFromBuild' toggle="true"/]

    [@ww.textarea labelKey='artifactory.task.gradle.artifactSpecs' name='builder.artifactoryGradleBuilder.artifactSpecs' rows='10' cols='200' wrap='off' cssClass="long-field"/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.release.enableReleaseManagement' name='enableReleaseManagement' toggle='true'/]

[@ui.bambooSection dependsOn='enableReleaseManagement' showOn=true]
    [@ww.textfield labelKey='artifactory.task.release.vcsTagBase' name='builder.artifactoryGradleBuilder.vcsTagBase'/]
    [@ww.textfield labelKey='artifactory.task.release.gitReleaseBranch' name='builder.artifactoryGradleBuilder.gitReleaseBranch'/]
    [@ww.textfield labelKey='artifactory.task.release.releaseProps' name='builder.artifactoryGradleBuilder.releaseProps'/]
    [@ww.textfield labelKey='artifactory.task.release.nextIntegProps' name='builder.artifactoryGradleBuilder.nextIntegProps'/]
    [@ww.textfield labelKey='artifactory.task.release.gradle.alternativeTasks' name='builder.artifactoryGradleBuilder.alternativeTasks'/]
[/@ui.bambooSection]

[@ui.bambooSection titleKey='builder.common.tests.directory.description']
    [@ww.checkbox labelKey='builder.common.tests.exists' name='testChecked' toggle='true'/]

    [@ui.bambooSection dependsOn='testChecked' showOn=true]
        [@ww.radio labelKey='builder.common.tests.directory' name='testDirectoryOption'
        listKey='key' listValue='value' toggle='true'
        list=testDirectoryTypes ]
        [/@ww.radio]
        [@ui.bambooSection dependsOn='testDirectoryOption' showOn='customTestDirectory']
            [@ww.textfield labelKey='builder.common.tests.directory.custom' name='builder.artifactoryGradleBuilder.testResultsDirectory' /]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
[/@ui.bambooSection]
</div>

<script>
    function displayGradleArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('gradleArtifactoryConfigDiv');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('builder.artifactoryGradleBuilder.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadGradleResolvingRepoKeys(serverId)
            loadGradlePublishRepoKeys(serverId)
        }
    }

    function loadGradlePublishRepoKeys(serverId) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true',
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document
                        .getElementsByName('builder.artifactoryGradleBuilder.publishingRepo')[0];
                repoSelect.innerHTML = '';
                if (serverId >= 0) {

                    var selectedRepoKey = '${selectedPublishingRepoKey}';

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
                var errorMessage = 'An error has occurred while retrieving the publishing repository list.\n' +
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

    function loadGradleResolvingRepoKeys(serverId) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&resolvingRepos=true',
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document
                        .getElementsByName('builder.artifactoryGradleBuilder.resolutionRepo')[0];
                repoSelect.innerHTML = '';
                if (serverId >= 0) {

                    var selectedRepoKey = '${selectedResolutionRepoKey}';

                    var blankOption = document.createElement('option');
                    blankOption.innerHTML =
                            '-- To use Artifactory for resolution select a virtual repository --';
                    blankOption.value = 'noResolutionRepoKeyConfigured';
                    repoSelect.appendChild(blankOption);

                    for (var i = 0, l = json.length; i < l; i++) {
                        var deployableRepoKey = json[i];
                        var option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        repoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey == selectedRepoKey)) {
                            repoSelect.selectedIndex = (i + 1);
                        }
                    }
                }
            },
            error:function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the resolving repository list.\n' +
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
    displayGradleArtifactoryConfigs(${selectedServerId});
</script>