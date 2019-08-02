<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>
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

[@ww.select name='builder.artifactoryGradleBuilder.artifactoryServerId' labelKey='artifactory.task.gradle.artifactoryServerId' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayGradleArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]

<div id="gradleArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryGradleBuilder.resolutionRepo' labelKey='artifactory.task.gradle.resolutionRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true'/]

[@ww.select name='builder.artifactoryGradleBuilder.publishingRepo' labelKey='artifactory.task.gradle.publishingRepo' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true'/]
<div id="variableAsUserNotificationDiv" class="aui-message aui-message-warning warning shadowed"
     style="display: none; width: 80%; font-size: 80%" />

[@ww.textfield labelKey='artifactory.task.gradle.deployerUsername' name='builder.artifactoryGradleBuilder.deployerUsername'/]

[@ww.password labelKey='artifactory.task.gradle.deployerPassword' name='builder.artifactoryGradleBuilder.deployerPassword' showPassword='true'/]
[#--The Dummy password is a workaround for the autofill (Chrome)--]
[@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]

[@ww.checkbox labelKey='artifactory.task.gradle.useArtifactoryGradlePlugin' name='builder.artifactoryGradleBuilder.useArtifactoryGradlePlugin' toggle='true'/]

    [@ww.checkbox name='buildInfoAggregation' toggle='true' cssStyle='visibility:hidden; position: absolute'/]

    [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=true]
        [@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]
        [@ui.bambooSection dependsOn='captureBuildInfo' id="captureBuildInfoSet" showOn=true]
            [#include 'editEnvVarsSnippet.ftl'/]
            [#include 'editGradleSnippet.ftl'/]
        [#--blackduck integration--]
            [#include 'editBlackDuckBuilderSnippet.ftl'/]
        [/@ui.bambooSection]
    [/@ui.bambooSection]
    [@ui.bambooSection dependsOn='buildInfoAggregation' showOn=false]
        [@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='publishBuildInfo' toggle='true'/]
        [@ui.bambooSection dependsOn='publishBuildInfo' id="publishBuildInfoSet" showOn=true]
            [#include 'editEnvVarsSnippet.ftl'/]
            [#include 'editGradleSnippet.ftl'/]
        [#--blackduck integration--]
            [#include 'editBlackDuckBuilderSnippet.ftl'/]
        [/@ui.bambooSection]

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
    [#include 'vcsConfiguration.ftl'/]
[/@ui.bambooSection]

[@ww.checkbox labelKey="Bintray configuration (deprecated)" name="bintrayConfiguration" toggle='true'/]

[@ui.bambooSection dependsOn="bintrayConfiguration"  showOn=true]
    [@ww.textfield name="bintray.subject" labelKey="artifactory.task.pushToBintray.subject"/]
    [@ww.textfield name="bintray.repository" labelKey="artifactory.task.pushToBintray.repository"/]
    [@ww.textfield name="bintray.packageName" labelKey="artifactory.task.pushToBintray.packageName"/]
    [@ww.textfield name="bintray.version" labelKey="artifactory.task.pushToBintray.version"/]
    [@ww.textfield name="bintray.licenses" labelKey="artifactory.task.pushToBintray.licenses"/]
    [@ww.textfield name="bintray.vcsUrl" labelKey="artifactory.task.pushToBintray.vcsUrl"/]
    [@ww.select name="bintray.signMethod" label="Sign method" list=signMethods listKey='key' listValue='value'/]
    [@ww.textfield name="bintray.gpgPassphrase" labelKey= "GPG Passphrase"/]
    [@ww.checkbox name="bintray.mavenSync" labelKey="Maven Central Sync"/]
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

<script type="text/javascript">

    function displayGradleArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('gradleArtifactoryConfigDiv');
        var credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        
        var variableExpression = RegExp('\\$\\{bamboo\\..*}');
        
        if(variableExpression.test(credentialsUserName) || variableExpression.test(credentialsPassword)){
			credentialsUserName = "";
			credentialsPassword = "";
			
			var variableMessage = 'The users repositories cannot be loaded when using a variable as username or password. Using globally available repositories instead';
        	var notificationDiv = document.getElementById('variableAsUserNotificationDiv');
        	
        	notificationDiv.innerHTML += variableMessage;
        	notificationDiv.style.display = '';
        }

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
            loadGradleResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword)
            loadGradlePublishRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadGradlePublishRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                    '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
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
                errorDiv.innerHTML += errorMessage;
                errorDiv.style.display = '';
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
                var repoSelect = document
                        .getElementsByName('builder.artifactoryGradleBuilder.resolutionRepo')[0];
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
                errorDiv.innerHTML += errorMessage;
                errorDiv.style.display = '';
            }
        });
    }
    var errorDiv = document.getElementById('artifactory-error');
    errorDiv.innerHTML = '';

    displayGradleArtifactoryConfigs(${selectedServerId});
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