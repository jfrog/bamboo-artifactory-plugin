<html>
<head>
    <meta name="tab" content="Artifactory Release Management"/>
    <meta name="decorator" content="result"/>
    <style type="text/css">
        .log .line {

        }
    </style>
</head>

<body>
<div class="section">
    <h2>Build Info</h2>

    <img width="48px" hight="48px" src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-icon.png"/>
    <a href="${artifactoryReleaseManagementUrl}" target="_blank">Artifactory Build Info</a>
</div>
[#if releaseBuild && permittedToPromote]
<div class="section">
    <h2>Artifactory Release Management</h2>

    <img width="48px" hight="48px"
         src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-release.png"/>
    <span>Artifactory Pro Release Promotion</span>
</div>
    [#if promoting]
        [@ww.form action='promote' submitLabelKey='Update'
        titleKey='Promote'
        descriptionKey='Promote'
        showActionErrors='false']
            [@ww.hidden name='planKey' value=planKey /]
            [@ww.hidden name='buildKey' value=buildKey /]
            [@ww.hidden name='buildNumber' value=buildNumber /]
            [@ww.hidden name='returnUrl' value=returnUrl /]
            [@ww.hidden name='baseUrl' value=baseUrl /]
            [@ww.hidden name='artifactoryReleaseManagementUrl' value=artifactoryReleaseManagementUrl /]
            [@ww.select labelKey='Target Status' name='target' list=promotionTargets /]
            [@ww.textfield labelKey='Comment' name = 'comment' value='${comment}'/]
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
                    url:'${req.contextPath}/getLog.action',
                    dataType:'html',
                    cache:false,
                    success:function (html) {
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
</body>
</html>