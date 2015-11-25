<div id="artifactory-error" class="aui-message aui-message-error error shadowed"
     style="display: none; width: 80%; font-size: 80%"></div>
[@ui.bambooSection titleKey='artifactory.task.generic.deploy.title']
    [@ww.select name='builder.artifactoryGenericBuilder.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayGenericArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
<div id="genericArtifactoryConfigDiv">
    [@ww.select name='builder.artifactoryGenericBuilder.deployableRepo' labelKey='artifactory.task.maven.targetRepo' list=dummyList listKey='repoKey' listValue='repoKey' toggle='true'/]

    [#--The Dummy tags are workaround for the autocomplete (Chorme)--]
    [@ww.password name='artifactory.generic.username.DUMMY' cssStyle='display: none;'/]
    [@ww.textfield name='artifactory.generic.username' labelKey='artifactory.task.maven.deployerUsername'/]

    [@ww.password name='artifactory.generic.password.DUMMY' cssStyle='display: none;'/]
    [@ww.password name='artifactory.generic.password' labelKey='artifactory.task.maven.deployerPassword' showPassword='true'/]

    [@ww.textarea name='artifactory.generic.deployPattern' labelKey='artifactory.task.generic.deployPattern' rows='10' cols='80' cssClass="long-field" /]

    [@ww.checkbox labelKey='artifactory.task.publishBuildInfo' name='artifactory.generic.publishBuildInfo' toggle='true' /]

    [@ui.bambooSection dependsOn='artifactory.generic.publishBuildInfo' showOn=true]

    [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='artifactory.generic.includeEnvVars' toggle='true'/]

    [@ui.bambooSection dependsOn='artifactory.generic.includeEnvVars' showOn=true]
        [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='artifactory.generic.envVarsIncludePatterns' /]
        [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='artifactory.generic.envVarsExcludePatterns' /]
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

[/@ui.bambooSection]
</div>
[/@ui.bambooSection]

<script>
    function displayGenericArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('genericArtifactoryConfigDiv');
        var credentialsUserName = configDiv.getElementsByTagName('input')[2].value;
        var credentialsPassword = configDiv.getElementsByTagName('input')[4].value;

        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('builder.artifactoryGenericBuilder.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }

            loadGenericRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadGenericRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
            '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                var repoSelect = document.getElementsByName('builder.artifactoryGenericBuilder.deployableRepo')[0];
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
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                var errorMessage = 'An error has occurred while retrieving the target repository list.\n' +
                        'Response: ' + XMLHttpRequest.status + ', ' + XMLHttpRequest.statusText + '.\n';
                if (XMLHttpRequest.status == 404) {
                    errorMessage +=
                            'Please make sure that the Artifactory Server Configuration Management Servlet is accesible.'
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
    displayGenericArtifactoryConfigs(${selectedServerId});
</script>