[@ww.textfield labelKey='Project File' name='builder.artifactoryMaven3Builder.projectFile' /]
[@ww.textarea labelKey='Goals' name='builder.artifactoryMaven3Builder.goal' rows='4' required='true' /]
[@ww.textarea labelKey='Additional Maven Parameters' name='builder.artifactoryMaven3Builder.additionalMavenParams' rows='2' required='false' /]

[#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
[@ww.select labelKey='builder.common.jdk' name='builder.artifactoryMaven3Builder.buildJdk' cssClass="jdkSelectWidget"
list=uiConfigBean.jdkLabels required='true'
extraUtility=addJdkLink /]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='maven'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryMaven3Builder.executable'
list=uiConfigBean.getExecutableLabels('maven') extraUtility=addExecutableLink required='true' /]

[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryMaven3Builder.environmentVariables'
descriptionKey='Space-separated key-value pairs of extra environment variables to pass to the build process (e.g. EXT_PATH=/var/lib/ext).'/]
[@ww.textfield labelKey='Maven Opts' name='builder.artifactoryMaven3Builder.mavenOpts'
descriptionKey='Space-separated parameters to pass as MAVEN_OPTS (e.g.: -Dmaven.repo.local=/mnt/work). Note: MAVEN_OPTS added as System Environment Variables will be ignored!'/]
[@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryMaven3Builder.workingSubDirectory' helpUri='working-directory.ftl' /]

[@ww.checkbox labelKey='Resolve artifacts from Artifactory' name='resolveFromArtifacts' toggle='true'
descriptionKey="Check if you wish all dependency resolution to go through Artifactory. <br/> Notice: this will override any external repository definition in Maven settings or POM files."/]
[@ui.bambooSection dependsOn='resolveFromArtifacts' showOn=true]
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionArtifactoryServerId' labelKey='Resolution Artifactory Server URL' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayResolutionMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true'
    descriptionKey='Select an Artifactory server.'/]
<div id="maven3ArtifactoryResolutionConfigDiv">
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionRepo' labelKey='Resolution repository' list=dummyList
    listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey=''/]
[@ww.textfield labelKey='Resolver Username' name='builder.artifactoryMaven3Builder.resolverUsername'
descriptionKey='Name of a user with read permissions on the target repository.'/]
[@ww.password labelKey='Resolver Password' name='builder.artifactoryMaven3Builder.resolverPassword' showPassword='true'
descriptionKey='password of a user with read permissions on the target repository.'/]
</div>
[/@ui.bambooSection]


[@ww.select name='builder.artifactoryMaven3Builder.artifactoryServerId' labelKey='Artifactory Server URL' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true'
descriptionKey='Select an Artifactory server.'/]

<div id="maven3ArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryMaven3Builder.deployableRepo' labelKey='Target Repository' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey='Select a target deployment repository.'/]

[@ww.textfield labelKey='Deployer Username' name='builder.artifactoryMaven3Builder.deployerUsername'
descriptionKey='Name of a user with deployment permissions on the target repository.'/]

[@ww.password labelKey='Deployer Password' name='builder.artifactoryMaven3Builder.deployerPassword' showPassword='true'
descriptionKey='The password of the user entered above.'/]

[@ww.checkbox labelKey='Deploy Maven Artifacts' name='deployMavenArtifacts' toggle='true'
descriptionKey="Uncheck if you do not wish to deploy Maven artifacts from the plugin (a more efficient alternative to Maven's own 'deploy' goal)."/]

[@ui.bambooSection dependsOn='deployMavenArtifacts' showOn=true]
    [@ww.textfield labelKey='Deployment Include Patterns'
    name='builder.artifactoryMaven3Builder.deployIncludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be included in publishing. Include patterns are applied on the published file path before any
    exclude patterns.'/]
    [@ww.textfield labelKey='Deployment Exclude Patterns'
    name='builder.artifactoryMaven3Builder.deployExcludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be excluded from publishing. Exclude patterns are applied on the published file path before any
    exclude patterns.'/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='Capture and Publish Build Info' name='publishBuildInfo'
toggle='true' descriptionKey='Check if you wish to publish build information to Artifactory.'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='Include Environment Variables' name='includeEnvVars'
    toggle='true' descriptionKey='Check if you wish to include all environment variables accessible by the builds process.'/]

    [@ui.bambooSection dependsOn='includeEnvVars' showOn=true]
        [@ww.textfield labelKey='Environment Variables Include Patterns'
        name='envVarsIncludePatterns'
        descriptionKey='Comma or space-separated list of patterns of environment variables that will be included in publishing
        (may contain the * and the ? wildcards). Include patterns are applied on the published build info before any exclude patterns.'/]
        [@ww.textfield labelKey='Environment Variables Exclude Patterns'
        name='envVarsExcludePatterns'
        descriptionKey='Comma or space-separated list of patterns of environment variables that will be included in publishing
        (may contain the * and the ? wildcards). Exclude patterns are applied on the published build info after any include patterns.'/]
    [/@ui.bambooSection]

    [@ww.checkbox labelKey='Run License Checks (Requires Pro)' name='runLicenseChecks'
    toggle='true' descriptionKey='Check if you wish that automatic license scanning will occur after build is complete.'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='Send License Violation Notifications to'
        name='builder.artifactoryMaven3Builder.licenseViolationRecipients' descriptionKey='Whitespace-separated list of recipient addresses.'/]

        [@ww.textfield labelKey='Limit Checks To The Following Scopes'
        name='builder.artifactoryMaven3Builder.limitChecksToScopes' descriptionKey='Space-separated list of scopes.'/]

        [@ww.checkbox labelKey='Include Published Artifacts' name='builder.artifactoryMaven3Builder.includePublishedArtifacts'
        toggle='true' descriptionKey="Include the build's published module artifacts in the license violation checks if they are also used
                    as dependencies for other modules in this build."/]

        [@ww.checkbox labelKey='Disable Automatic License Discovery' name='builder.artifactoryMaven3Builder.disableAutoLicenseDiscovery'
        toggle='true' descriptionKey="Tells Artifactory to not try and automatically analyze and tag the build's dependencies with license information
                    upon deployment. You can still attach license information manually by running 'Auto-Find' from the build's
                    Licenses tab in Artifactory."/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ww.checkbox labelKey='Enable Release Management' name='enableReleaseManagement' toggle='true'
descriptionKey='Enable Release Management to Artifactory'/]

[@ui.bambooSection dependsOn='enableReleaseManagement' showOn=true]
    [@ww.textfield labelKey='VCS Tags Base URL/Name' name='builder.artifactoryMaven3Builder.vcsTagBase'
    descriptionKey='For subversion this is the URL of the tags location, for Git and Perforce this is the name of the tag/label.'/]
    [@ww.textfield labelKey='Git Release Branch Name Prefix' name='builder.artifactoryMaven3Builder.gitReleaseBranch'
    descriptionKey='The prefix of the release branch name (applicable only to Git).'/]
    [@ww.textfield labelKey='Alternative Maven Tasks and Options' name='builder.artifactoryMaven3Builder.alternativeTasks'
    descriptionKey='Alternative Maven and options to execute for a Maven build running as part of the release. If left empty, the build will use original tasks and options instead of replacing them. '/]
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