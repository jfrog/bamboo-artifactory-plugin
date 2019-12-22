[#import "/lib/build.ftl" as bd]

[@ww.label labelKey='Command' name='artifactory.task.npm.command.choice' hideOnNull='true'/]
[@ww.label labelKey='Npm arguments' name='artifactory.task.npm.command.install' hideOnNull='true'/]
[@ww.label labelKey='Working subdirectory with package.json' name='artifactory.task.npm.workingFolder' hideOnNull='true'/]
[#--[@bd.showJdk /]--]
[@ww.label labelKey='Executable' name='artifactory.task.npm.executable' /]
[@ww.label labelKey='builder.common.env' name='environmentVariables' hideOnNull='true' /]

<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='artifactory.task.npm.artifactoryServerId' /]
[@ww.label labelKey='Npm Source Repository' name='artifactory.task.npm.resolutionRepo' hideOnNull='true' /]
[@ww.label labelKey='Npm Target Repository' name='artifactory.task.npm.publishingRepo' hideOnNull='true' /]

[@ww.label labelKey='Capture Build Info' name='captureBuildInfo' hideOnNull='true' /]
[#if isCaptureBuildInfo]
    [@ww.label labelKey='Include Environment Variables' name='includeEnvVars' hideOnNull='true' /]
[/#if]

</div>

<script>
    var urlLabel = document.getElementById('artifactory_task_npm_artifactoryServerId');
    if (urlLabel) {
        var serverId = urlLabel.innerHTML;
        if (-1 != serverId) {
            var configDiv = document.getElementById('artifactoryConfigDiv');
            configDiv.style.display = 'block';
            urlLabel.innerHTML = '${selectedServerUrl}';
        }
    }
</script>