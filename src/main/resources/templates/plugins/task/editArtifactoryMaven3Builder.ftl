<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>
[@ww.textfield labelKey='artifactory.task.maven.projectFile' name='builder.artifactoryMaven3Builder.projectFile' /]
[@ww.textarea labelKey='artifactory.task.maven.goals' name='builder.artifactoryMaven3Builder.goal' rows='4' required='true' /]
[@ww.textarea labelKey='artifactory.task.maven.additionalMavenParams' name='builder.artifactoryMaven3Builder.additionalMavenParams' rows='2' required='false' /]

<div id="buildJdkSelectionDiv">
    [#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
    [@ww.select labelKey='artifactory.task.maven.buildJdk' name='builder.artifactoryMaven3Builder.buildJdk' cssClass="jdkSelectWidget"
    list=uiConfigBean.jdkLabels required='true'
    extraUtility=addJdkLink /]
</div>
<div id="buildJdkOverridenDiv">
</div>

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='maven'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryMaven3Builder.executable'
list=uiConfigBean.getExecutableLabels('maven') extraUtility=addExecutableLink required='true' /]

[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryMaven3Builder.environmentVariables' /]
[@ww.textfield labelKey='artifactory.task.maven.mavenOpts' name='builder.artifactoryMaven3Builder.mavenOpts' /]
[@ww.textfield labelKey='builder.common.sub' name='builder.artifactoryMaven3Builder.workingSubDirectory' /]

[@ww.checkbox labelKey='artifactory.task.maven.resolveFromArtifacts' name='resolveFromArtifacts' toggle='true' /]
[@ui.bambooSection dependsOn='resolveFromArtifacts' showOn=true]
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionArtifactoryServerId' labelKey='artifactory.task.maven.resolutionArtifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayResolutionMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true' /]
<div id="maven3ArtifactoryResolutionConfigDiv">
    [@ww.select name='builder.artifactoryMaven3Builder.resolutionRepo' labelKey='artifactory.task.maven.resolutionRepo' list=dummyList
    listKey='repoKey' listValue='repoKey' toggle='true' /]

[@ww.textfield labelKey='artifactory.task.maven.resolverUsername' name='builder.artifactoryMaven3Builder.resolverUsername'/]

[@ww.password labelKey='artifactory.task.maven.resolverPassword' name='builder.artifactoryMaven3Builder.resolverPassword' showPassword='true' /]
[#--The Dummy password is a workaround for the autofill (Chrome)--]
[@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

</div>
[/@ui.bambooSection]


[@ww.select name='builder.artifactoryMaven3Builder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayMaven3ArtifactoryConfigs(this.value)' emptyOption=true toggle='true' /]

<div id="maven3ArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryMaven3Builder.deployableRepo' labelKey='artifactory.task.maven.targetRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' /]

[@ww.textfield labelKey='artifactory.task.maven.deployerUsername' name='builder.artifactoryMaven3Builder.deployerUsername' /]

[@ww.password labelKey='artifactory.task.maven.deployerPassword' name='builder.artifactoryMaven3Builder.deployerPassword' showPassword='true'/]
[#--The Dummy password is a workaround for the autofill (Chrome)--]
[@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

[@ww.checkbox labelKey='artifactory.task.maven.deployMavenArtifacts' name='deployMavenArtifacts' toggle='true' /]

[@ui.bambooSection dependsOn='deployMavenArtifacts' showOn=true]
    [@ww.textfield labelKey='artifactory.task.deployIncludePatterns' name='builder.artifactoryMaven3Builder.deployIncludePatterns' /]
    [@ww.textfield labelKey='artifactory.task.deployExcludePatterns' name='builder.artifactoryMaven3Builder.deployExcludePatterns' /]
    [@ww.checkbox labelKey='artifactory.task.filterExcludedArtifactsFromBuild' name='builder.artifactoryMaven3Builder.filterExcludedArtifactsFromBuild' toggle="true"/]
[/@ui.bambooSection]
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
[@ww.checkbox labelKey='artifactory.task.release.enableReleaseManagement' name='enableReleaseManagement' toggle='true'/]

[@ui.bambooSection dependsOn='enableReleaseManagement' showOn=true]
    [@ww.textfield labelKey='artifactory.task.release.vcsTagBase' name='builder.artifactoryMaven3Builder.vcsTagBase'/]
    [@ww.textfield labelKey='artifactory.task.release.gitReleaseBranch' name='builder.artifactoryMaven3Builder.gitReleaseBranch'/]
    [@ww.textfield labelKey='artifactory.task.release.alternativeTasks' name='builder.artifactoryMaven3Builder.alternativeTasks'/]
    [#include 'vcsConfiguration.ftl'/]
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
        var credentialsUserName = configDiv.getElementsByTagName('input')[1].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[2].value;

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
            loadMaven3RepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function displayResolutionMaven3ArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('maven3ArtifactoryResolutionConfigDiv');
        var credentialsUserName = configDiv.getElementsByTagName('input')[1].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[2].value;

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
            loadMaven3ResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadMaven3RepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
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

    function loadMaven3ResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url:'${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&resolvingRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType:'json',
            cache:false,
            success:function (json) {
                var repoSelect = document.getElementsByName('builder.artifactoryMaven3Builder.resolutionRepo')[0];
                repoSelect.innerHTML = '';
                if (serverId >= 0) {

                    var selectedRepoKey = '${selectedResolutionRepoKey}';

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
                var errorMessage = 'An error has occurred while retrieving the resolving repository list.\n' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.\n';
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
    displayMaven3ArtifactoryConfigs(${selectedServerId});
    displayResolutionMaven3ArtifactoryConfigs(${selectedResolutionArtifactoryServerId});
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