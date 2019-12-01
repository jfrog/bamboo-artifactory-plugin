[#import "/lib/build.ftl" as bd]
<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='artifactory.task.collectIssues.artifactoryServerId' /]
[@ww.label labelKey='Config Json' name='artifactory.task.collectIssues.configSource.taskConfiguration' hideOnNull='true' /]
[@ww.label labelKey='Config Path' name='artifactory.task.collectIssues.configSource.file' hideOnNull='true' /]
</div>
