[#--Build--]
[@ui.bambooSection titleKey='artifactory.task.buildConfigurationsTitle' collapsible=true]
    [@ww.textfield labelKey='artifactory.task.gradle.switches' name='builder.artifactoryGradleBuilder.switches'/]
    [@ww.textfield labelKey='artifactory.task.gradle.tasks' name='builder.artifactoryGradleBuilder.tasks'/]
    [@ww.textfield labelKey='artifactory.task.gradle.buildScript' name='builder.artifactoryGradleBuilder.buildScript'/]
    [@ww.textfield labelKey='artifactory.task.gradle.buildFile' name='builder.artifactoryGradleBuilder.buildFile'/]

    <div id="buildJdkSelectionDiv">
        [#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
        [@ww.select labelKey='builder.common.jdk' name='builder.artifactoryGradleBuilder.buildJdk' cssClass="jdkSelectWidget"
        list=uiConfigBean.jdkLabels required='true'
        extraUtility=addJdkLink /]
    </div>
    <div id="buildJdkOverridenDiv">
    </div>

    [@ww.checkbox labelKey='artifactory.task.gradle.useGradleWrapper' name='builder.artifactoryGradleBuilder.useGradleWrapper' toggle='true'/]
    [@ui.bambooSection dependsOn='builder.artifactoryGradleBuilder.useGradleWrapper' showOn=true]
        [@ww.textfield labelKey='artifactory.task.gradle.gradleWrapperLocation' name='builder.artifactoryGradleBuilder.gradleWrapperLocation' /]
    [/@ui.bambooSection]

    [@ui.bambooSection dependsOn='builder.artifactoryGradleBuilder.useGradleWrapper' showOn=false]
        [#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='gradle'/][/#assign]
        [@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryGradleBuilder.executable'
        list=uiConfigBean.getExecutableLabels('gradle') extraUtility=addExecutableLink required='true'/]
    [/@ui.bambooSection]

    [@ww.textfield labelKey='builder.common.env' name='builder.artifactoryGradleBuilder.environmentVariables' /]
    [@ww.checkbox labelKey='artifactory.task.gradle.useArtifactoryGradlePlugin' name='builder.artifactoryGradleBuilder.useArtifactoryGradlePlugin' toggle='true'/]
    [@ui.bambooSection titleKey='builder.common.tests.directory.description']
        [@ww.checkbox labelKey='builder.common.tests.exists' name='testChecked' toggle='true'/]

        [@ui.bambooSection dependsOn='testChecked' showOn=true]
            [@ww.radio labelKey='builder.common.tests.directory' name='testDirectoryOption'
            listKey='key' listValue='value' toggle='true' list=testDirectoryTypes /]

            [@ui.bambooSection dependsOn='testDirectoryOption' showOn='customTestDirectory']
                [@ww.textfield labelKey='builder.common.tests.directory.custom' name='builder.artifactoryGradleBuilder.testResultsDirectory' /]
            [/@ui.bambooSection]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[#--Server Selection--]
[@ui.bambooSection id="serverSelectSection" titleKey='artifactory.task.serverSelection' collapsible=true]
    [@ww.select name='builder.artifactoryGradleBuilder.artifactoryServerId' labelKey='artifactory.task.gradle.artifactoryServerId' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayGradleArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
    [@ww.textfield labelKey='artifactory.task.gradle.deployerUsername' name='builder.artifactoryGradleBuilder.deployerUsername' onchange='javascript: overridingCredentialsChanged()'/]
    [@ww.password labelKey='artifactory.task.gradle.deployerPassword' name='builder.artifactoryGradleBuilder.deployerPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged()'/]
[#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
[/@ui.bambooSection]

<div id="gradleArtifactoryConfigDiv">
    [#--Resolution--]
    [@ui.bambooSection titleKey='artifactory.task.resolutionConfigurationsTitle' collapsible=true]
        [@ww.select name='builder.artifactoryGradleBuilder.resolutionRepo' labelKey='artifactory.task.gradle.resolutionRepo' list=dummyList
        listKey='repoKey' listValue='repoKey' toggle='true'/]
        <div id="resolve-repo-error" class="aui-message aui-message-error error shadowed"
             style="display: none; width: 80%; font-size: 80%" />
    [/@ui.bambooSection]

    [#--Deployment--]
    [@ui.bambooSection titleKey='artifactory.task.deploymentConfigurationsTitle' collapsible=true]
        [@ww.select name='builder.artifactoryGradleBuilder.publishingRepo' labelKey='artifactory.task.gradle.publishingRepo' list=dummyList
        listKey='repoKey' listValue='repoKey' toggle='true'/]
        <div id="publish-repo-error" class="aui-message aui-message-error error shadowed"
             style="display: none; width: 80%; font-size: 80%" />
        [@ww.checkbox labelKey='artifactory.task.gradle.publishArtifacts' name='publishArtifacts' toggle='true'/]
        [@ui.bambooSection dependsOn='publishArtifacts' showOn=true]
            [@ww.select labelKey="artifactory.task.gradle.publishForkCount" name="builder.artifactoryGradleBuilder.publishForkCount" list="builder.artifactoryGradleBuilder.publishForkCountList" value='builder.artifactoryGradleBuilder.publishForkCount'/]
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
    [/@ui.bambooSection]
</div>

[#--Build Info and Release Management--]
[@ui.bambooSection titleKey='artifactory.task.buildInfoReleaseManagementTitle' collapsible=true]
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
        [@ww.textfield labelKey='artifactory.task.release.vcsTagBase' name='builder.artifactoryGradleBuilder.vcsTagBase'/]
        [@ww.textfield labelKey='artifactory.task.release.gitReleaseBranch' name='builder.artifactoryGradleBuilder.gitReleaseBranch'/]
        [@ww.textfield labelKey='artifactory.task.release.releaseProps' name='builder.artifactoryGradleBuilder.releaseProps'/]
        [@ww.textfield labelKey='artifactory.task.release.nextIntegProps' name='builder.artifactoryGradleBuilder.nextIntegProps'/]
        [@ww.textfield labelKey='artifactory.task.release.gradle.alternativeTasks' name='builder.artifactoryGradleBuilder.alternativeTasks'/]
        [#include 'vcsConfiguration.ftl'/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

<script type="text/javascript">

    function displayGradleArtifactoryConfigs(serverId) {
        var serverSelectDiv = document.getElementById('serverSelectSection');
        var credentialsUserName = serverSelectDiv.getElementsByTagName('input')[1].value;
        var credentialsPassword = serverSelectDiv.getElementsByTagName('input')[2].value;

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
            loadGradleResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword);
            loadGradlePublishRepoKeys(serverId, credentialsUserName, credentialsPassword);
        }
    }

    function loadGradlePublishRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                publishRepoSelect.innerHTML = '';
                if (serverId >= 0) {
                    var selectedRepoKey = '${selectedPublishingRepoKey}';
                    for (var i = 0, l = json.length; i < l; i++) {
                        var deployableRepoKey = json[i];
                        var option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        publishRepoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey == selectedRepoKey)) {
                            publishRepoSelect.selectedIndex = i;
                        }
                    }
                }
                publishErrorDiv.innerHTML = '';
                publishErrorDiv.style.display = 'none';
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the publishing repository list.<br>' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accessible.'
                } else {
                    errorMessage +=
                            'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }
                errorMessage += "<br>";
                publishErrorDiv.innerHTML = errorMessage;
                publishErrorDiv.style.display = '';
                publishRepoSelect.innerHTML = '';
            }
        });
    }

    function loadGradleResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&resolvingRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                resolveRepoSelect.innerHTML = '';
                if (serverId >= 0) {
                    var selectedRepoKey = '${selectedResolutionRepoKey}';
                    for (var i = 0, l = json.length; i < l; i++) {
                        var deployableRepoKey = json[i];
                        var option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        resolveRepoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey == selectedRepoKey)) {
                            resolveRepoSelect.selectedIndex = i;
                        }
                    }
                }
                resolveErrorDiv.innerHTML = '';
                resolveErrorDiv.style.display = 'none';
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the resolving repository list.<br>' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accessible.'
                } else {
                    errorMessage +=
                            'Please check the server logs for error messages from the Artifactory Server Configuration Management Servlet.'
                }
                errorMessage += "<br>";
                resolveErrorDiv.innerHTML = errorMessage;
                resolveErrorDiv.style.display = '';
                resolveRepoSelect.innerHTML = '';
            }
        });
    }

    function overridingCredentialsChanged() {
        var serverIdSection = document.getElementById("serverSelectSection");
        var selectedServer = serverIdSection.getElementsByTagName('select')[0].value;
        displayGradleArtifactoryConfigs(selectedServer);
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

    var resolveErrorDiv = document.getElementById('resolve-repo-error');
    var publishErrorDiv = document.getElementById('publish-repo-error');
    var publishRepoSelect = document.getElementsByName('builder.artifactoryGradleBuilder.publishingRepo')[0];
    var resolveRepoSelect = document.getElementsByName('builder.artifactoryGradleBuilder.resolutionRepo')[0];

    // Init error-divs.
    publishErrorDiv.innerHTML = '';
    publishErrorDiv.style.display = 'none';
    resolveErrorDiv.innerHTML = '';
    resolveErrorDiv.style.display = 'none';

    displayGradleArtifactoryConfigs(${selectedServerId});
    displayRequiredFieldset();

</script>