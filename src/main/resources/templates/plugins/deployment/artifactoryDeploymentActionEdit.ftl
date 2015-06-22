[@ui.bambooSection titleKey='artifactory.task.deploy.title']
    [@ww.select name='artifactory.deployment.artifactoryServerId' labelKey='artifactory.task.maven.artifactoryServerUrl' list=serverConfigManager.allServerConfigs
    listKey='id' listValue='url' onchange='javascript: displayDeployArtifactoryConfigs(this.value)' emptyOption=true toggle='true'/]
<div id="deployArtifactoryConfiguration">
    [@ww.select name='artifactory.deployment.deploymentRepository' labelKey='artifactory.task.maven.targetRepo' list=dummyList listKey='repoKey' listValue='repoKey' toggle='true'/]
[#--The Dummy tags are workaround for the autocomplete (Chrome)--]
    [@ww.password name='artifactory.deployment.username.DUMMY' cssStyle='display: none;'/]
    [@ww.textfield name='artifactory.deployment.username' labelKey='artifactory.task.maven.deployerUsername'/]

    [@ww.password name='artifactory.deployment.password.DUMMY' cssStyle='display: none;'/]
    [@ww.password name='artifactory.deployment.password' labelKey='artifactory.task.maven.deployerPassword' showPassword='true'/]
</div>
[/@ui.bambooSection]

<script>
    function displayDeployArtifactoryConfigs(serverId) {
        var configDiv = document.getElementById('deployArtifactoryConfiguration');
        var credentialsUserName = document.getElementsByName("artifactory.deployment.username")[0].value;
        var credentialsPassword = document.getElementsByName('artifactory.deployment.password')[0].value;

        if ((serverId == null) || (serverId.length == 0) || (-1 == serverId)) {
            configDiv.style.display = 'none';
        } else {
            configDiv.style.display = 'block';
            var urlSelect = document.getElementsByName('artifactory.deployment.artifactoryServerId')[0];
            var urlOptions = urlSelect.options;
            for (var i = 0; i < urlOptions.length; i++) {
                var option = urlOptions[i];
                if (option.value == '' + serverId) {
                    urlSelect.selectedIndex = i;
                    break;
                }
            }

            loadRepoKeys(serverId, credentialsUserName, credentialsPassword)
        }
    }

    function loadRepoKeys(serverId, credentialsUserName, credentialsPassword) {
        AJS.$.ajax({
            url: '${req.contextPath}/plugins/servlet/artifactoryConfigServlet?serverId=' + serverId +
            '&deployableRepos=true&user=' + credentialsUserName + '&password=' + credentialsPassword,
            dataType: 'json',
            cache: false,
            success: function (json) {
                var repoSelect = document.getElementsByName('artifactory.deployment.deploymentRepository')[0];
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
                alert(errorMessage);
            }
        });
    }

    displayDeployArtifactoryConfigs(${selectedServerId});
</script>