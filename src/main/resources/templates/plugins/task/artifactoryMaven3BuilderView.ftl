[#import "/lib/build.ftl" as bd]
[@ww.head theme='ajax'/]
[@ww.label labelKey='Executable' name='builder.artifactoryMaven3Builder.executable' /]
[@ww.label labelKey='Project File' name='builder.artifactoryMaven3Builder.projectFile' hideOnNull='true' /]
[@ww.label labelKey='Goals' name='builder.artifactoryMaven3Builder.goal' /]
[@ww.label labelKey='Additional Maven Parameters' name='builder.artifactoryMaven3Builder.additionalMavenParams' hideOnNull='true'/]
[#--[@ui.displayJdk jdkLabel=buildJdk isJdkValid=uiConfigBean.isJdkLabelValid(buildJdk) /]--]
[@ww.label labelKey='builder.common.env' name='builder.artifactoryMaven3Builder.environmentVariables' hideOnNull='true'/]
[@ww.label labelKey='Maven Opts' name='builder.artifactoryMaven3Builder.mavenOpts' hideOnNull='true'/]
[@ww.label labelKey='builder.common.sub' name='builder.artifactoryMaven3Builder.workingSubDirectory' hideOnNull='true' /]

<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='builder.artifactoryMaven3Builder.artifactoryServerId' /]
[@ww.label labelKey='Target Repository' name='builder.artifactoryMaven3Builder.deployableRepo' hideOnNull='true' /]
[@ww.label labelKey='Deployer Username' name='builder.artifactoryMaven3Builder.deployerUsername' hideOnNull='true' /]
[@ww.label labelKey='Deploy Maven Artifacts' name='builder.artifactoryMaven3Builder.deployMavenArtifacts'/]
[#if isPublishArtifacts]
[@ww.label labelKey='Deployment Include Patterns' name='builder.artifactoryMaven3Builder.deployIncludePatterns'/]
[@ww.label labelKey='Deployment Exclude Patterns' name='builder.artifactoryMaven3Builder.deployExcludePatterns'/]
[@ww.label labelKey='Filter excluded artifacts from build Info' name='builder.artifactoryMaven3Builder.filterExcludedArtifactsFromBuild'/]
[/#if]
[@ww.label labelKey='Run License Checks (Requires Pro)' name='builder.artifactoryMaven3Builder.builder.runLicenseChecks'/]
[#if isRunLicenseChecks]
[@ww.label labelKey='Send License Violation Notifications To'
name='builder.artifactoryMaven3Builder.licenseViolationRecipients' hideOnNull='true' /]
[@ww.label labelKey='Limit Checks To The Following Scopes'
name='builder.artifactoryMaven3Builder.limitChecksToScopes' hideOnNull='true' /]
[@ww.label labelKey='Include Published Artifacts' name='builder.artifactoryMaven3Builder.includePublishedArtifacts'/]
[@ww.label labelKey='Disable Automatic License Discovery' name='builder.artifactoryMaven3Builder.disableAutoLicenseDiscovery'/]
[/#if]
</div>

[#--[#if build.buildDefinition.builder.hasTests()]
[@ww.label labelKey='builder.common.tests.directory' name='builder.artifactoryMaven3Builder.testResultsDirectory' hideOnNull='true' /]
[/#if]--]

<script>
    var urlLabel = document.getElementById('builder_artifactoryMaven3Builder_artifactoryServerId');
    if (urlLabel) {
        var serverId = urlLabel.innerHTML;
        if (-1 != serverId) {
            var configDiv = document.getElementById('artifactoryConfigDiv');
            configDiv.style.display = 'block';
            urlLabel.innerHTML = '${selectedServerUrl}';
        }
    }
</script>