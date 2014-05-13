[#import "/lib/build.ftl" as bd]

[@ww.label labelKey='Build File' name='builder.artifactoryIvyBuilder.buildFile' /]
[@ww.label labelKey='Targets' name='builder.artifactoryIvyBuilder.target' /]
[#--[@bd.showJdk /]--]
[@ww.label labelKey='Executable' name='builder.artifactoryIvyBuilder.executable' /]
[@ww.label labelKey='builder.common.env' name='builder.artifactoryIvyBuilder.environmentVariables' hideOnNull='true' /]
[@ww.label labelKey='Ant Ops' name='builder.artifactoryIvyBuilder.antOpts' hideOnNull='true' /]
[@ww.label labelKey='builder.common.sub' name='builder.artifactoryIvyBuilder.workingSubDirectory' hideOnNull='true' /]

<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='builder.artifactoryIvyBuilder.artifactoryServerId' /]
[@ww.label labelKey='Target Repository' name='builder.artifactoryIvyBuilder.deployableRepo' hideOnNull='true' /]
[@ww.label labelKey='Deployer Username' name='builder.artifactoryIvyBuilder.deployerUsername' hideOnNull='true' /]
[@ww.label labelKey='Deploy Artifacts' name='builder.artifactoryIvyBuilder.deployArtifacts'/]
[#if isPublishArtifacts]
[@ww.label labelKey='Deployment Include Patterns' name='builder.artifactoryIvyBuilder.deployIncludePatterns'/]
[@ww.label labelKey='Deployment Exclude Patterns' name='builder.artifactoryIvyBuilder.deployExcludePatterns'/]
[@ww.label labelKey='Filter excluded artifacts from build Info' name='builder.artifactoryIvyBuilder.filterExcludedArtifactsFromBuild'/]
[/#if]
[@ww.label labelKey='Use Maven 2 Compatible Patterns' name='builder.artifactoryIvyBuilder.useM2CompatiblePatterns'/]

    [#if isUseM2CompatiblePatterns]
[@ww.label labelKey='Ivy Pattern' name='builder.artifactoryIvyBuilder.ivyPattern'/]
[@ww.label labelKey='Artifact Pattern' name='builder.artifactoryIvyBuilder.artifactPattern'/]
[/#if]
[@ww.label labelKey='Run License Checks (Requires Pro)' name='builder.artifactoryIvyBuilder.runLicenseChecks'/]
[#if isRunLicenseChecks]
[@ww.label labelKey='Send License Violation Notifications To'
name='builder.artifactoryIvyBuilder.licenseViolationRecipients' hideOnNull='true' /]
[@ww.label labelKey='Limit Checks To The Following Scopes'
name='builder.artifactoryIvyBuilder.limitChecksToScopes' hideOnNull='true' /]
[@ww.label labelKey='Include Published Artifacts' name='builder.artifactoryIvyBuilder.includePublishedArtifacts'/]
[@ww.label labelKey='Disable Automatic License Discovery' name='builder.artifactoryIvyBuilder.disableAutoLicenseDiscovery'/]
[/#if]
</div>

[#if hasTests]
[@ww.label labelKey='builder.common.tests.directory' name='builder.artifactoryIvyBuilder.testResultsDirectory' hideOnNull='true' /]
[/#if]

<script>
    var urlLabel = document.getElementById('builder_artifactoryIvyBuilder_artifactoryServerId');
    if (urlLabel) {
        var serverId = urlLabel.innerHTML;
        if (-1 != serverId) {
            var configDiv = document.getElementById('artifactoryConfigDiv');
            configDiv.style.display = 'block';
            urlLabel.innerHTML = '${selectedServerUrl}';
        }
    }
</script>