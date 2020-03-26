[#import "/lib/build.ftl" as bd]
[@ww.head theme='ajax'/]
[@ww.label labelKey='Executable' name='builder.artifactoryMaven3Builder.executable' /]
[@ww.label labelKey='Goals' name='builder.artifactoryMaven3Builder.goal' /]
[@ww.label labelKey='Additional Maven Parameters' name='builder.artifactoryMaven3Builder.additionalMavenParams' hideOnNull='true'/]
[@ww.label labelKey='builder.common.sub' name='builder.artifactoryMaven3Builder.workingSubDirectory' hideOnNull='true' /]
[@ww.label labelKey='Project File' name='builder.artifactoryMaven3Builder.projectFile' hideOnNull='true' /]
[@ww.label labelKey='builder.common.env' name='builder.artifactoryMaven3Builder.environmentVariables' hideOnNull='true'/]
[@ww.label labelKey='Maven Opts' name='builder.artifactoryMaven3Builder.mavenOpts' hideOnNull='true'/]

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
</div>

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