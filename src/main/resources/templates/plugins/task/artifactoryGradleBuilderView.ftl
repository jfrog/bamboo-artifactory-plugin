[#import "/lib/build.ftl" as bd]

[@ww.label labelKey='Switches' name='builder.artifactoryGradleBuilder.switches' hideOnNull='true'/]
[@ww.label labelKey='Tasks' name='builder.artifactoryGradleBuilder.tasks' hideOnNull='true'/]
[@ww.label labelKey='Effective Build Script' name='builder.artifactoryGradleBuilder.buildScript' hideOnNull='true'/]
[@ww.label labelKey='Build File' name='builder.artifactoryGradleBuilder.buildFile' hideOnNull='true'/]
[#--[@bd.showJdk /]--]
[@ww.label labelKey='Executable' name='builder.artifactoryGradleBuilder.executable' /]
[@ww.label labelKey='builder.common.env' name='environmentVariables' hideOnNull='true' /]

<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='builder.artifactoryGradleBuilder.artifactoryServerId' /]
[@ww.label labelKey='Resolution Repository' name='builder.artifactoryGradleBuilder.resolutionRepo' hideOnNull='true' /]
[@ww.label labelKey='Publishing Repository' name='builder.artifactoryGradleBuilder.publishingRepo' hideOnNull='true' /]

[@ww.label labelKey='Deployer Username' name='builder.artifactoryGradleBuilder.deployerUsername' hideOnNull='true' /]

[@ww.label labelKey='Capture And Publish Build Info' name='publishBuildInfo' hideOnNull='true' /]
[#if isPublishBuildInfo]
    [@ww.label labelKey='Include Environment Variables' name='includeEnvVars' hideOnNull='true' /]

    [@ww.label labelKey='Run License Checks (Requires Pro)' name='builder.artifactoryGradleBuilder.runLicenseChecks'/]
    [#if isRunLicenseChecks]
        [@ww.label labelKey='Send License Violation Notifications To'
        name='builder.artifactoryGradleBuilder.licenseViolationRecipients' hideOnNull='true' /]
        [@ww.label labelKey='Limit Checks To The Following Scopes'
        name='builder.artifactoryGradleBuilder.limitChecksToScopes' hideOnNull='true' /]
        [@ww.label labelKey='Include Published Artifacts' name='builder.artifactoryGradleBuilder.includePublishedArtifacts'/]
        [@ww.label labelKey='Disable Automatic License Discovery' name='builder.artifactoryGradleBuilder.disableAutoLicenseDiscovery'/]
    [/#if]
[/#if]

[@ww.label labelKey='Publish Artifacts To Artifactory' name='builder.artifactoryGradleBuilder.publishArtifacts'/]
[#if isPublishArtifacts]
    [@ww.label labelKey='Publish Maven Descriptors' name='builder.artifactoryGradleBuilder.publishMavenDescriptors'/]
    [@ww.label labelKey='Publish Ivy Descriptors' name='builder.artifactoryGradleBuilder.publishIvyDescriptors'/]
    [@ww.label labelKey='Use Maven 2 Compatible Patterns' name='builder.artifactoryGradleBuilder.useM2CompatiblePatterns'/]

    [#if isUseM2CompatiblePatterns]
        [@ww.label labelKey='Ivy Pattern' name='builder.artifactoryGradleBuilder.ivyPattern'/]
        [@ww.label labelKey='Artifact Pattern' name='builder.artifactoryGradleBuilder.artifactPattern'/]
    [/#if]

    [@ww.label labelKey='Publication Include Patterns' name='builder.artifactoryGradleBuilder.publishIncludePatterns'/]
    [@ww.label labelKey='Publication Exclude Patterns' name='builder.artifactoryGradleBuilder.publishExcludePatterns'/]
    [@ww.label labelKey='Filter excluded artifacts from build Info' name='builder.artifactoryGradleBuilder.filterExcludedArtifactsFromBuild'/]
    [@ww.label labelKey='Artifact Specs' name='builder.artifactoryGradleBuilder.artifactSpecs'/]
[/#if]

</div>

<script>
    var urlLabel = document.getElementById('builder_artifactoryGradleBuilder_artifactoryServerId');
    if (urlLabel) {
        var serverId = urlLabel.innerHTML;
        if (-1 != serverId) {
            var configDiv = document.getElementById('artifactoryConfigDiv');
            configDiv.style.display = 'block';
            urlLabel.innerHTML = '${selectedServerUrl}';
        }
    }
</script>