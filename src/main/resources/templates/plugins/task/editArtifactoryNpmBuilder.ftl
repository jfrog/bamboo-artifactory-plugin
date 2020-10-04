[@ww.select labelKey='artifactory.task.npm.header.command.choice' name='artifactory.task.npm.command.choice' listKey='key' listValue='value' toggle='true' list=npmCommandOptions/]
[@ui.bambooSection dependsOn='artifactory.task.npm.command.choice' showOn='install']
    [@ww.textfield labelKey='artifactory.task.npm.header.install.npmArguments' name='artifactory.task.npm.install.npmArguments'/]
[/@ui.bambooSection]

[@ww.textfield labelKey='artifactory.task.npm.header.workingSubdirectory' name='artifactory.task.npm.workingSubdirectory'/]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='npm'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='artifactory.task.npm.executable'
list=uiConfigBean.getExecutableLabels('npm') extraUtility=addExecutableLink required='true'/]
[@ww.textfield labelKey='builder.common.env' name='artifactory.task.npm.environmentVariables' /]

[@ui.bambooSection id="resolutionSection" dependsOn='artifactory.task.npm.command.choice' showOn='install']
    [@ww.select labelKey='artifactory.task.npm.header.resolutionArtifactoryServerId' name='artifactory.task.npm.resolutionArtifactoryServerId' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayResolutionNpmArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
    <div id="npmArtifactoryResolvingConfigDiv">
        [@ww.select labelKey='artifactory.task.npm.header.resolutionRepo' name='artifactory.task.npm.resolutionRepo' list=dummyList
        listKey='repoKey' listValue='repoKey' toggle='true'/]
        <div id="resolve-repo-error" class="aui-message aui-message-error error shadowed"
             style="display: none; width: 80%; font-size: 80%"/>
        [@ww.select labelKey='Override credentials' name='resolver.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield labelKey='artifactory.task.npm.header.resolverUsername' name='artifactory.task.npm.resolverUsername' onchange='javascript: overridingCredentialsChanged("resolution")'/]
            [@ww.password labelKey='artifactory.task.npm.header.resolverPassword' name='artifactory.task.npm.resolverPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged("resolution")'/]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='resolver.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='resolver.sharedCredentials' labelKey='artifactory.task.generic.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]
    </div>
[/@ui.bambooSection]


[@ui.bambooSection id="publishSection" dependsOn='artifactory.task.npm.command.choice' showOn='pack and publish']
    [@ww.select labelKey='artifactory.task.npm.header.artifactoryServerId' name='artifactory.task.npm.artifactoryServerId' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayPublishingNpmArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
    <div id="npmArtifactoryPublishingConfigDiv">
        [@ww.select labelKey='artifactory.task.npm.header.publishingRepo' name='artifactory.task.npm.publishingRepo' list=dummyList
        listKey='repoKey' listValue='repoKey' toggle='true'/]
        <div id="publish-repo-error" class="aui-message aui-message-error error shadowed"
             style="display: none; width: 80%; font-size: 80%"/>
        [@ww.select labelKey='Override credentials' name='deployer.overrideCredentialsChoice' listKey='key' listValue='value' toggle='true' list=overrideCredentialsOptions/]
        [#--  No credentials overriding  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='noOverriding'/]
        [#--  Username and password  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='usernamePassword']
            [@ww.textfield labelKey='artifactory.task.npm.header.deployerUsername' name='artifactory.task.npm.deployerUsername' onchange='javascript: overridingCredentialsChanged("publish")'/]
            [@ww.password labelKey='artifactory.task.npm.header.deployerPassword' name='artifactory.task.npm.deployerPassword' showPassword='true' onchange='javascript: overridingCredentialsChanged("publish")'/]
        [/@ui.bambooSection]
        [#--  Use shared credentials  --]
        [@ui.bambooSection dependsOn='deployer.overrideCredentialsChoice' showOn='sharedCredentials']
            [@ww.select name='deployer.sharedCredentials' labelKey='artifactory.task.generic.sharedCredentials' list=credentialsAccessor.allCredentials
            listKey='name' listValue='name' toggle='true'/]
        [/@ui.bambooSection]
    </div>
[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.captureBuildInfo' name='captureBuildInfo' toggle='true'/]
[@ui.bambooSection dependsOn='captureBuildInfo' showOn=true]
    [#include 'editEnvVarsSnippet.ftl'/]
[/@ui.bambooSection]


<script type="text/javascript">

    function displayResolutionNpmArtifactoryConfigs(serverId) {
        let configDiv = document.getElementById('npmArtifactoryResolvingConfigDiv');
        let credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        let credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        if ((serverId == null) || (serverId.length === 0) || (-1 === serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            let urlSelect = document.getElementsByName('artifactory.task.npm.resolutionArtifactoryServerId')[0];
            let urlOptions = urlSelect.options;
            for (let i = 0; i < urlOptions.length; i++) {
                let option = urlOptions[i];
                if (option.value === '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadNpmResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadNpmResolvingRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                '&resolvingRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                resolveRepoSelect.innerHTML = '';
                if (serverId >= 0) {
                    let selectedRepoKey = '${selectedResolutionRepoKey}';
                    for (let i = 0, l = json.length; i < l; i++) {
                        let deployableRepoKey = json[i];
                        let option = document.createElement('option');
                        option.innerHTML = deployableRepoKey;
                        option.value = deployableRepoKey;
                        resolveRepoSelect.appendChild(option);
                        if (selectedRepoKey && (deployableRepoKey === selectedRepoKey)) {
                            resolveRepoSelect.selectedIndex = i;
                        }
                    }
                }
                resolveErrorDiv.innerHTML = '';
                resolveErrorDiv.style.display = 'none';
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                let errorMessage = 'An error has occurred while retrieving the resolving repository list.<br>' +
                    'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status === 404) {
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

    function displayPublishingNpmArtifactoryConfigs(serverId) {
        let configDiv = document.getElementById('npmArtifactoryPublishingConfigDiv');
        let credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        let credentialsPassword = configDiv.getElementsByTagName('input')[3].value;
        if ((serverId == null) || (serverId.length === 0) || (-1 === serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            let urlSelect = document.getElementsByName('artifactory.task.npm.artifactoryServerId')[0];
            let urlOptions = urlSelect.options;
            for (let i = 0; i < urlOptions.length; i++) {
                let option = urlOptions[i];
                if (option.value === '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }
            loadNpmPublishRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadNpmPublishRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
                '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                publishRepoSelect.innerHTML = '';
                if (serverId >= 0) {
                    let selectedRepoKey = '${selectedPublishingRepoKey}';
                    for (let i = 0, l = json.length; i < l; i++) {
                        let deployableRepoKey = json[i];
                        let option = document.createElement('option');
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
                let errorMessage = 'An error has occurred while retrieving the publishing repository list.<br>' +
                    'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.<br>';
                if (XMLHttpRequest.status === 404) {
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
            let serverIdSection = document.getElementById("publishSection");
            let selectedServerId = serverIdSection.getElementsByTagName('select')[0].value;
            displayPublishingNpmArtifactoryConfigs(selectedServerId);
        } else if (repositoryType === "resolution") {
            let serverIdSection = document.getElementById("resolutionSection");
            let selectedServerId = serverIdSection.getElementsByTagName('select')[0].value;
            displayResolutionNpmArtifactoryConfigs(selectedServerId);
        }
    }

    let resolveErrorDiv = document.getElementById('resolve-repo-error');
    let publishErrorDiv = document.getElementById('publish-repo-error');
    let publishRepoSelect = document.getElementsByName('artifactory.task.npm.publishingRepo')[0];
    let resolveRepoSelect = document.getElementsByName('artifactory.task.npm.resolutionRepo')[0];

    // Init error-divs.
    publishErrorDiv.innerHTML = '';
    publishErrorDiv.style.display = 'none';
    resolveErrorDiv.innerHTML = '';
    resolveErrorDiv.style.display = 'none';

    displayPublishingNpmArtifactoryConfigs(${selectedPublishingServerId});
    displayResolutionNpmArtifactoryConfigs(${selectedResolutionServerId});
</script>