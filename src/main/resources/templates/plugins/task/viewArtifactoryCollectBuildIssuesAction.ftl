[#import "/lib/build.ftl" as bd]
<div id="artifactoryConfigDiv" style='display: none'>
[@ww.label labelKey='Artifactory Server URL' name='artifactory.task.collectBuildIssues.artifactoryServerId' /]
[@ww.label labelKey='Config Json' name='artifactory.task.collectBuildIssues.config.source.taskConfiguration' hideOnNull='true' /]
[@ww.label labelKey='Config File Path' name='artifactory.task.collectBuildIssues.config.source.file' hideOnNull='true' /]
</div>
