<html>
<head>
    <title>${plan.name}: Artifactory Pro Release Staging</title>
</head>

<body>
<img width="48px" height="48px"
     src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-release.png"/>
[#assign singlePlan = false]
[#assign sort = false]
[#assign targetAction = 'releaseBuild']
[@ww.form action=targetAction submitLabelKey='Build and Release to Artifactory'
titleKey='Artifactory Pro Release Staging' showActionErrors='false']
    [@ww.hidden name='planKey' value=planKey /]
<div class="section">
    [#if gradle]
        [#include "fragments/gradleVersions.ftl"/]
    [#elseif maven]
        [#include "fragments/mavenVersions.ftl"/]
    [#else]
        Only Gradle and Maven types are supported
    [/#if]
</div>
[/@ww.form]
</body>
</html>
