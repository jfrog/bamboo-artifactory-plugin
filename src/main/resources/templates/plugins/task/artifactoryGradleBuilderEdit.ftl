[@ww.textfield labelKey='Command-Line Options' name='builder.artifactoryGradleBuilder.switches'
descriptionKey='Gradle
<a href="http://gradle.org/latest/docs/userguide/tutorial_gradle_command_line.html" target="_blank">command-line options</a> to invoke.'/]

[@ww.textfield labelKey='Tasks' name='builder.artifactoryGradleBuilder.tasks' descriptionKey='Gradle tasks to invoke.'/]

[@ww.textfield labelKey='Build Script Directory' name='builder.artifactoryGradleBuilder.buildScript'
descriptionKey='If the build script is not located in the root of the build directory, specify the path (relative to the
build directory).'/]

[@ww.textfield labelKey='Build Script File' name='builder.artifactoryGradleBuilder.buildFile'
descriptionKey='If your gradle build script is not named build.gradle, specify the gradle build name script.'/]

[#assign addJdkLink][@ui.displayAddJdkInline /][/#assign]
[@ww.select labelKey='builder.common.jdk' name='builder.artifactoryGradleBuilder.buildJdk' cssClass="jdkSelectWidget"
list=uiConfigBean.jdkLabels required='true'
extraUtility=addJdkLink /]

[@ww.checkbox labelKey='Use Gradle Wrapper' name='builder.artifactoryGradleBuilder.useGradleWrapper' toggle='true'/]

[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='gradle'/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='executable.type' name='builder.artifactoryGradleBuilder.executable'
list=uiConfigBean.getExecutableLabels('gradle') extraUtility=addExecutableLink required='true' /]

[@ww.textfield labelKey='builder.common.env' name='builder.artifactoryGradleBuilder.environmentVariables'
descriptionKey='Space-separated key-value pairs of extra environment variables to pass to the build process (e.g. EXT_PATH=/var/lib/ext).' /]

[@ww.select name='builder.artifactoryGradleBuilder.artifactoryServerId' labelKey='Artifactory Server URL' list=serverConfigManager.allServerConfigs
listKey='id' listValue='url' onchange='javascript: displayGradleArtifactoryConfigs(this.value)' emptyOption=true toggle='true'
descriptionKey='Select an Artifactory server.'/]

<div id="gradleArtifactoryConfigDiv">
[@ww.select name='builder.artifactoryGradleBuilder.resolutionRepo' labelKey='Resolution Repository' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey='Select a resolution repository.'/]

[@ww.select name='builder.artifactoryGradleBuilder.publishingRepo' labelKey='Publishing Repository' list=dummyList
listKey='repoKey' listValue='repoKey' toggle='true' descriptionKey='Select a publishing repository.'/]

[@ww.textfield labelKey='Deployer Username' name='builder.artifactoryGradleBuilder.deployerUsername'
descriptionKey='Name of a user with deployment permissions on the target repository.'/]

[@ww.password labelKey='Deployer Password' name='builder.artifactoryGradleBuilder.deployerPassword' showPassword='true'
descriptionKey='The password of the user entered above.'/]

[@ww.checkbox labelKey='Project uses the Artifactory Gradle Plugin:' name='builder.artifactoryGradleBuilder.useArtifactoryGradlePlugin' toggle='true'
descriptionKey='The Bamboo plugin automatically applies the Artifactory plugin (and, consequently, the artifactoryPublish task) to all projects.
Check this flag to have Bamboo skip this step if your project is already using the Artifactory plugin or the artifactoryPublish task directly. All elements in this job configuration will override matching project-level configuration elements.
If your project applies the Artifactory plugin using a custom init script, make sure to include your init script as part of the list of Gradle switches.'/]

[@ww.checkbox labelKey='Capture and Publish Build Info' name='publishBuildInfo'
toggle='true' descriptionKey='Check if you wish to publish build information to Artifactory.'/]

[@ui.bambooSection dependsOn='publishBuildInfo' showOn=true]
    [@ww.checkbox labelKey='Include All Environment Variables' name='builder.artifactoryGradleBuilder.includeEnvVars'
    toggle='true' descriptionKey='Check if you wish to include all environment variables accessible by the builds process.'/]

    [@ww.checkbox labelKey='Run License Checks (Requires Pro)' name='runLicenseChecks'
    toggle='true' descriptionKey='Check if you wish that automatic license scanning will occur after build is complete.'/]

    [@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
        [@ww.textfield labelKey='Send License Violation Notifications to'
        name='builder.artifactoryGradleBuilder.licenseViolationRecipients' descriptionKey='Whitespace-separated list of recipient addresses.'/]

        [@ww.textfield labelKey='Limit Checks to The Following Scopes'
        name='builder.artifactoryGradleBuilder.limitChecksToScopes' descriptionKey='Space-separated list of scopes.'/]

        [@ww.checkbox labelKey='Include Published Artifacts' name='builder.artifactoryGradleBuilder.includePublishedArtifacts'
        toggle='true' descriptionKey="Include the build's published module artifacts in the license violation checks if they are also used
                as dependencies for other modules in this build."/]

        [@ww.checkbox labelKey='Disable Automatic License Discovery' name='builder.artifactoryGradleBuilder.disableAutoLicenseDiscovery'
        toggle='true' descriptionKey="Tells Artifactory to not try and automatically analyze and tag the build's dependencies with license information
                upon deployment. You can still attach license information manually by running 'Auto-Find' from the build's
                Licenses tab in Artifactory."/]
    [/@ui.bambooSection]
[/@ui.bambooSection]

[@ww.checkbox labelKey='Publish Artifacts to Artifactory' name='publishArtifacts' toggle='true'
descriptionKey='Check if you wish to publish produced build artifacts to Artifactory.'/]

[@ui.bambooSection dependsOn='publishArtifacts' showOn=true]

    [@ww.checkbox labelKey='Publish Maven Descriptors' name='builder.artifactoryGradleBuilder.publishMavenDescriptors' toggle='true'
    descriptionKey='Check if you wish to publish Gradle-generated POM files to Artifactory. Note: Maven descriptors are always deployed according to the Maven layout convention.'/]

    [@ww.checkbox labelKey='Publish Ivy Descriptors' name='builder.artifactoryGradleBuilder.publishIvyDescriptors' toggle='true'
    descriptionKey='Check if you wish to publish Gradle-generated ivy.xml descriptor files to Artifactory.'/]

    [@ww.checkbox labelKey='Use Maven 2 Compatible Patterns' name='useM2CompatiblePatterns' toggle='true'
    descriptionKey='Whether to use the default Maven 2 patterns when publishing artifacts and Ivy descriptors, or to use custom patterns.'/]

    [@ui.bambooSection dependsOn='useM2CompatiblePatterns' showOn='false']
        [@ww.textfield labelKey='Ivy Pattern' name='builder.artifactoryGradleBuilder.ivyPattern'
        descriptionKey='The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a> to use for published Ivy descriptors.'/]
        [@ww.textfield labelKey='Artifact Pattern' name='builder.artifactoryGradleBuilder.artifactPattern'
        descriptionKey='The <a href="http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns">pattern</a> to use for published artifacts.'/]
    [/@ui.bambooSection]

    [@ww.textfield labelKey='Publication Include Patterns'
    name='builder.artifactoryGradleBuilder.publishIncludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be included in publishing. Include patterns are applied on the published file path before any
    exclude patterns.'/]
    [@ww.textfield labelKey='Publication Exclude Patterns'
    name='builder.artifactoryGradleBuilder.publishExcludePatterns'
    descriptionKey='Comma or space-separated list of
    <a href="http://ant.apache.org/manual/dirtasks.html#patterns" target="_blank">Ant-style patterns</a>
    of files that will be excluded from publishing. Exclude patterns are applied on the published file path before any
    exclude patterns.'/]

    [@ww.textarea labelKey='Artifact Properties' name='builder.artifactoryGradleBuilder.artifactSpecs' rows='10' cols='200'
    wrap='off' cssClass="long-field" descriptionKey="A line-separated list of properties to attach to deployed artifacts.<br>
    Each lines specifies filtering rules by which to apply properties, in the format of:<br><br>
    <i>configurationName artifactFilter properties</i><br><br>
    <table border='0'>
    <tbody><tr>
    <td><i>configurationName: </i></td><td>The Gradle configuration. You can specify all to apply for all configurations.</td></tr>
    <tr><td><i>artifactFilter:</i></td><td>a filter in the format of group:artifact:version:classifier@ext - all fileds are mandatory and can take Ant-like wildcard patterns using * and ?. For example: org.acme:*:1.0.?_*:*@tgz</td></tr>
    <tr><td><i>properties:</i></td><td>a list of properties in the format of key1:val1, key2:val2, key3:val3<br></td>
    </tr></tbody></table><br>"/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='Enable Release Management' name='enableReleaseManagement' toggle='true'
descriptionKey='Enable Release Management to Artifactory'/]

[@ui.bambooSection dependsOn='enableReleaseManagement' showOn=true]
    [@ww.textfield labelKey='VCS Tags Base URL/Name' name='builder.artifactoryGradleBuilder.vcsTagBase'
    descriptionKey='For subversion this is the URL of the tags location, for Git and Perforce this is the name of the tag/label.'/]
    [@ww.textfield labelKey='Git Release Branch Name Prefix' name='builder.artifactoryGradleBuilder.gitReleaseBranch'
    descriptionKey='The prefix of the release branch name (applicable only to Git).'/]
    [@ww.textfield labelKey='Release Properties' name='builder.artifactoryGradleBuilder.releaseProps'
    descriptionKey='Properties in your projects gradle.properties file whose value should change upon release.'/]
    [@ww.textfield labelKey='Next Integration Properties' name='builder.artifactoryGradleBuilder.nextIntegProps'
    descriptionKey='Properties in your projects gradle.properties file whose value should change upon release, but also for work on the next integration/development version after the release has been created. '/]
    [@ww.textfield labelKey='Alternative Gradle Tasks and Options' name='builder.artifactoryGradleBuilder.alternativeTasks'
    descriptionKey='Alternative tasks and options to execute for a Gradle build running as part of the release. If left empty, the build will use original tasks and options instead of replacing them. '/]
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