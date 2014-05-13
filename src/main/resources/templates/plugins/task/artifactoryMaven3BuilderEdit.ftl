[@ww.textfield labelKey='artifactory.task.maven.projectFile' name='builder.artifactoryMaven3Builder.projectFile' /]
[@ww.textarea labelKey='artifactory.task.maven.goals' name='builder.artifactoryMaven3Builder.goal' rows='4' required='true' /]
[@ww.textarea labelKey='artifactory.task.maven.additionalMavenParams' name='builder.artifactoryMaven3Builder.additionalMavenParams' rows='2' required='false' /]

[#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
[@ww.select labelKey='builder.common.jdk' name='builder.artifactoryMaven3Builder.buildJdk' cssClass="jdkSelectWidget"
list=uiConfigBean.jdkLabels required='true'
extraUtility=addJdkLink /]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='maven'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryMaven3Builder.executable'
list=uiConfigBean.getExecutableLabels('maven') extraUtility=addExecutableLink required='true' /]

[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryMaven3Builder.environmentVariables' /]
[@ww.textfield labelKey='artifactory.task.maven.mavenOpts' name='builder.artifactoryMaven3Builder.mavenOpts' /]
[@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryMaven3Builder.workingSubDirectory' helpUri='working-directory.ftl' /]

[@ww.checkbox labelKey='artifactory.task.maven.resolveFromArtifacts' name='resolveFromArtifacts' toggle='true' /]
[@ui.bambooSection dependsOn='resolveFromArtifacts' showOn=true]
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionArtifactoryServerId' labelKey='artifactory.task.maven.resolutionArtifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayResolutionMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true' /]
<div id="maven3ArtifactoryResolutionConfigDiv">
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionRepo' labelKey='artifactory.task.maven.resolutionRepo' list=dummyList
    listKey='repoKey' listValue='repoKey' toggle='true' /]
[@ww.textfield labelKey='artifactory.task.maven.resolverUsername' name='builder.artifactoryMaven3Builder.resolverUsername' /]
[@ww.password labelKey='artifactory.task.maven.resolverPassword' name='builder.artifactoryMaven3Builder.resolverPassword' showPassword='true' /]
</div>
[/@ui.bambooSection]


[@ww.select name='builder.artifactoryMaven3Builder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true' /]

<div id="maven3ArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryMaven3Builder.deployableRepo' labelKey='artifactory.task.maven.targetRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' /]

[@ww.textfield labelKey='artifactory.task.maven.deployerUsername' name='builder.artifactoryMaven3Builder.deployerUsername' /]

[@ww.password labelKey='artifactory.task.maven.deployerPassword' name='builder.artifactoryMaven3Builder.deployerPassword' showPassword='true'/]

[@ww.checkbox labelKey='artifactory.task.maven.deployMavenArtifacts' name='deployMavenArtifacts' toggle='true' /]

[@ui.bambooSection dependsOn='deployMavenArtifacts' showOn=true]
    [@ww.textfield labelKey='artifactory.task.deployIncludePatterns' name='builder.artifactoryMaven3Builder.deployIncludePatterns' /]
    [@ww.textfield labelKey='artifactory.task.deployExcludePatterns' name='builder.artifactoryMaven3Builder.deployExcludePatterns' /]
    [@ww.checkbox labelKey='artifactory.task.filterExcludedArtifactsFromBuild' name='builder.artifactoryMaven3Builder.filterExcludedArtifactsFromBuild' toggle="true"/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='publishBuildInfo' toggle='true'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='includeEnvVars' toggle='true' /]

    [@ui.bambooSection dependsOn='includeEnvVars' showOn=true]
        [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='envVarsIncludePatterns'/]
        [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='envVarsExcludePatterns'/]
    [/@ui.bambooSection]

    [@ww.checkbox labelKey='artifactory.task.runLicenseChecks' name='runLicenseChecks' toggle='true'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryMaven3Builder.licenseViolationRecipients' /]

        [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryMaven3Builder.limitChecksToScopes' /]

        [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryMaven3Builder.includePublishedArtifacts' toggle='true'/]

        [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryMaven3Builder.disableAutoLicenseDiscovery' toggle='true'/]
    [/@ui.bambooSection]

    [#--blackduck integration--]
    [#include 'BlackDuckBuilderEditSnippet.ftl'/]

[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.release.enableReleaseManagement' name='enableReleaseManagement' toggle='true'/]

[@ui.bambooSection dependsOn='enableReleaseManagement' showOn=true]
    [@ww.textfield labelKey='artifactory.task.release.vcsTagBase' name='builder.artifactoryMaven3Builder.vcsTagBase'/]
    [@ww.textfield labelKey='artifactory.task.release.gitReleaseBranch' name='builder.artifactoryMaven3Builder.gitReleaseBranch'/]
    [@ww.textfield labelKey='artifactory.task.release.alternativeTasks' name='builder.artifactoryMaven3Builder.alternativeTasks'/]
[/@ui.bambooSection]


[@ui.bambooSection titleKey='builder.common.tests.directory.description']
    [@ww.checkbox labelKey='builder.common.tests.exists' name='testChecked' toggle='true'/]

    [@ui.bambooSection dependsOn='testChecked' showOn=true]
        [@ww.radio labelKey='builder.common.tests.directory' name='testDirectoryOption'
        listKey='key' listValue='value' toggle='true'
        list=testDirectoryTypes ]
        [/@ww.radio]
        [@ui.bambooSection dependsOn='testDirectoryOption' showOn='customTestDirectory']
            [@ww.textfield labelKey='builder.common.tests.directory.custom' name='builder.artifactoryMaven3Builder.testResultsDirectory' /]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
[/@ui.bambooSection]
</div>

<script>
    function displayMaven3ArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('maven3ArtifactoryConfigDiv');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('builder.artifactoryMaven3Builder.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadMaven3RepoKeys(serverId)
        }
    }

    function displayResolutionMaven3ArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('maven3ArtifactoryResolutionConfigDiv');
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document
                    .getElementsByName('builder.artifactoryMaven3Builder.resolutionArtifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadMaven3ResolvingRepoKeys(serverId)
        }
    }

    function loadMaven3RepoKeys(serverId) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true',
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document
                        .getElementsByName('builder.artifactoryMaven3Builder.deployableRepo')[0];
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

    function loadMaven3ResolvingRepoKeys(serverId) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&resolvingRepos=true',
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document.getElementsByName('builder.artifactoryMaven3Builder.resolutionRepo')[0];
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


    displayMaven3ArtifactoryConfigs(${selectedServerId});
    displayResolutionMaven3ArtifactoryConfigs(${selectedResolutionArtifactoryServerId});

</script>