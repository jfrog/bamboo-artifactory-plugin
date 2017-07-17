[#import "/lib/build.ftl" as bd]
<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='artifactory.deployment.artifactoryServerId' /]
[@ww.label labelKey='Publishing Repository' name='artifactory.deployment.deploymentRepository' hideOnNull='true' /]
[@ww.label labelKey='Deployer Username' name='artifactory.deployment.username' hideOnNull='true' /]
</div>
