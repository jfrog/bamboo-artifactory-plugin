<html>
<head>
    <title>${plan.name}: Artifactory Pro Release Staging</title>
    <meta name="tab" content="Artifactory Pro Release Staging"/>
    <meta name="decorator" content="result"/>
</head>

<body>


[#if releaseBuild && permittedToPromote]
<div class="section">

    <div class="form-view">
        <h2>
            <img style="margin-right: 10px; margin-bottom: 5px" width="48px" height="48px"
                 src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-release.png"/>Artifactory
            Release Promotion
        </h2>
    </div>
</div>
    [#if promoting]
        [@ww.form action='promote' submitLabelKey='Update' showActionErrors=false]
            [@ww.radio labelKey='Promotion Mode' name='promotionMode' toggle='true' list=supportedPromotionModes
            listKey='key' listValue='value' ]
            [/@ww.radio]
            [@ww.hidden name='planKey' value=planKey /]
            [@ww.hidden name='buildKey' value=buildKey /]
            [@ww.hidden name='buildNumber' value=buildNumber /]
            [@ww.hidden name='returnUrl' value=returnUrl /]
            [@ww.hidden name='baseUrl' value=baseUrl /]
            [@ww.hidden name='artifactoryReleaseManagementUrl' value=artifactoryReleaseManagementUrl /]
            [@ww.select labelKey='Target Status' name='target' list=promotionTargets /]
            [@ww.textfield labelKey='release.comment' name = 'comment' value='${comment}'/]
            [@ww.select name='promotionRepo' labelKey='Target promotion repository' list='promotionRepos'
            toggle='true' descriptionKey='Select a promotion repository.'/]
            [@ww.checkbox labelKey='Include dependencies' name='includeDependencies' toggle='true'/]
            [@ww.checkbox labelKey='Use Copy' name='useCopy' toggle='true'/]
        [/@ww.form]
    [#else ]
        [@ui.header pageKey='Promotion Log' /]

    <div id="releasePromotionLog" class="log"></div>

    <script type="text/javascript">
        (function () {
            var logInterval;

            function updateLog() {
                AJS.$.ajax({
                    url: '${req.contextPath}/getLog.action?buildKey=${buildKey}&buildNumber=${buildNumber}',
                    dataType: 'html',
                    cache: false,
                    success: function (html) {
                        var result = AJS.$(html);
                        if (result.filter('#promoteDone').length) {
                            clearInterval(logInterval);
                        }

                        AJS.$('#releasePromotionLog').html(result.filter('#promoteLog').html());
                    }
                });
            }

            updateLog();
            logInterval = setInterval(updateLog, 1000);
        })();
    </script>
    [/#if]
[/#if]

<div class="form-view">
    <h2>
        <img style="margin-right: 10px; margin-bottom: 5px" width="48px" height="48px"
             src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-release.png"/>Artifactory Pro
        Release Staging
    </h2>
</div>
[#assign singlePlan = false]
[#assign sort = false]
[#assign targetAction = 'releaseBuild']
[@ww.form action=targetAction submitLabelKey='artifactory.release.submit' showActionErrors='false']
    [@ww.hidden name='planKey' value=planKey /]
<div class="section">

    [#if gradle]
        [#include "fragments/gradleVersions.ftl"/]
        [#include "fragments/vcsConfiguration.ftl"/]
    [#elseif maven]
        [#include "fragments/mavenVersions.ftl"/]
        [#include "fragments/vcsConfiguration.ftl"/]
    [#else]
        Only Gradle and Maven types are supported
    [/#if]
</div>
[/@ww.form]
</body>
</html>
