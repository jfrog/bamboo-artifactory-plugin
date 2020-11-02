[@ww.select labelKey='artifactory.task.dotnet.header.command.choice' name='artifactory.task.dotnet.command.choice' listKey='key' listValue='value' toggle='true' list=commandOptions/]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='dotnet'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='artifactory.task.dotnet.executable'
list=uiConfigBean.getExecutableLabels('dotnet') extraUtility=addExecutableLink required='true'/]

[@ww.textfield labelKey='artifactory.task.dotnet.header.workingSubdirectory' name='artifactory.task.dotnet.workingSubdirectory'/]

[@ui.bambooSection dependsOn='artifactory.task.dotnet.command.choice' showOn='restore']
    [@ww.textfield labelKey='artifactory.task.dotnetCore.header.dotnetCoreArguments' name='artifactory.task.dotnet.arguments'/]
    [@ui.bambooSection id="resolutionSection"]
        [@ww.select labelKey='artifactory.task.dotnet.header.resolutionArtifactoryServerId' name='artifactory.task.dotnet.resolutionArtifactoryServerId' list=serverConfigManager.allServerConfigs
        listKey='id' listValue='url' onchange='javascript: displayResolutionDotnetCoreArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]

        <div id="dotnetCoreArtifactoryResolvingConfigDiv">
            [@ww.select labelKey='artifactory.task.dotnet.header.resolutionRepo' name='artifactory.task.dotnet.resolutionRepo' list=dummyList
            listKey='repoKey' listValue='repoKey' toggle='true'/]
            [@ui.bambooSection]
                <div id="resolve-repo-error" class="aui-message aui-message-error error shadowed"
                     style="display: none; width: 80%; font-size: 80%" />
            [/@ui.bambooSection]
            [@ww.select labelKey='artifactory.task.overrideCredentials' name='resolver.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
            [#--  No credentials overriding  --]
            [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='noOverriding'/]
            [#--  Username and password  --]
            [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='usernamePassword']
                [@ww.textfield labelKey='artifactory.task.dotnet.header.resolverUsername' name='artifactory.task.dotnet.resolverUsername' onchange='javascript: overridingCredentialsChanged("resolution")'/]
                [@ww.password labelKey='artifactory.task.dotnet.header.resolverPassword' name='artifactory.task.dotnet.resolverPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged("resolution")'/]
            [/@ui.bambooSection]
            [#--  Use shared credentials  --]
            [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='sharedCredentials']
                [@ww.select name='resolver.sharedCredentials' labelKey='artifactory.task.sharedCredentials' list=credentialsAccessor.allCredentials
                listKey='name' listValue='name' toggle='true'/]
            [/@ui.bambooSection]
        </div>
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ui.bambooSection id="publishSection" dependsOn='artifactory.task.dotnet.command.choice' showOn='push']
    [@ww.select labelKey='artifactory.task.dotnet.header.artifactoryServerId' name='artifactory.task.dotnet.artifactoryServerId' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayPublishingDotnetCoreArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
    <div id="dotnetCoreArtifactoryPublishingConfigDiv">
        [@ww.select labelKey='artifactory.task.dotnet.header.publishingRepo' name='artifactory.task.dotnet.publishingRepo' list=dummyList
        listKey='repoKey' listValue='repoKey' toggle='true'/]
        [@ui.bambooSection]
            <div id="publish-repo-error" class="aui-message aui-message-error error shadowed"
                 style="display: none; width: 80%; font-size: 80%" />
        [/@ui.bambooSection]
        [@ww.select labelKey='artifactory.task.overrideCredentials' name='deployer.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield labelKey='artifactory.task.dotnet.header.deployerUsername' name='artifactory.task.dotnet.deployerUsername' onchange='javascript: overridingCredentialsChanged("publish")'/]
            [@ww.password labelKey='artifactory.task.dotnet.header.deployerPassword' name='artifactory.task.dotnet.deployerPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged("publish")'/]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='deployer.sharedCredentials' labelKey='artifactory.task.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]
    </div>

    [@ww.textfield labelKey='artifactory.task.dotnet.header.pushTarget' name='artifactory.task.dotnet.pushTarget'/]

    [@ww.textfield labelKey='artifactory.task.dotnet.header.pushPattern' name='artifactory.task.dotnet.pushPattern'/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]
[@ui.bambooSection dependsOn='captureBuildInfo' showOn=true]
    [#include 'editBuildNameNumberSnippet.ftl'/]
    [#include 'editEnvVarsSnippet.ftl'/]
[/@ui.bambooSection]

<script type="text/javascript">

    function displayResolutionDotnetCoreArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('dotnetCoreArtifactoryResolvingConfigDiv');
        var credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('artifactory.task.dotnet.resolutionArtifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadDotnetCoreResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadDotnetCoreResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword) {
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

    function displayPublishingDotnetCoreArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('dotnetCoreArtifactoryPublishingConfigDiv');
        var credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('artifactory.task.dotnet.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadDotnetCorePublishRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadDotnetCorePublishRepoKeys(serverId, credentialsUserName, credentialsPassword) {
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

    function overridingCredentialsChanged(repositoryType) {
        if (repositoryType === "publish") {
            var serverIdSection = document.getElementById("publishSection");
            var selectedServerId = serverIdSection.getElementsByTagName('select')[0].value;
            displayPublishingDotnetCoreArtifactoryConfigs(selectedServerId);
        } else if (repositoryType === "resolution") {
            var serverIdSection = document.getElementById("resolutionSection");
            var selectedServerId = serverIdSection.getElementsByTagName('select')[0].value;
            displayResolutionDotnetCoreArtifactoryConfigs(selectedServerId);
        }
    }

    var resolveErrorDiv = document.getElementById('resolve-repo-error');
    var publishErrorDiv = document.getElementById('publish-repo-error');
    var resolveRepoSelect = document.getElementsByName('artifactory.task.dotnet.resolutionRepo')[0];
    var publishRepoSelect = document.getElementsByName('artifactory.task.dotnet.publishingRepo')[0];


    // Init error-divs.
    resolveErrorDiv.innerHTML = '';
    resolveErrorDiv.style.display = 'none';
    publishErrorDiv.innerHTML = '';
    publishErrorDiv.style.display = 'none';

    displayResolutionDotnetCoreArtifactoryConfigs(${selectedResolutionServerId});
    displayPublishingDotnetCoreArtifactoryConfigs(${selectedPublishingServerId});
</script>