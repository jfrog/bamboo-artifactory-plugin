<html>
<head>
    <title>Push to Bintray from Artifactory</title>
    <meta name="tab" content="Artifactory Push to Bintray"/>
    <meta name="decorator" content="result"/>
</head>
<body>

<div class="section">
    <div class="form-view">
        <h2>
            <img style="margin-right: 10px; margin-bottom: 5px" width="48px" height="48px"
                 src="${baseUrl}/download/resources/${groupId}.${artifactId}/bintray.png"/>
            Push to Bintray
        </h2>
    </div>
[#if pushing]
    [@ww.form action='push'  submitLabelKey="Push to Bintray"]

        [@ww.checkbox name="overrideDescriptorFile" labelKey="artifactory.task.pushToBintray.overrideDescriptor" value=false toggle='true'/]

        [@ww.hidden name='planName' value=planName /]
        [@ww.hidden name='planKey' value=planKey /]
        [@ww.hidden name='buildKey' value=buildKey /]
        [@ww.hidden name='buildNumber' value=buildNumber /]
        [@ww.hidden name='baseUrl' value=baseUrl /]

        [@ui.bambooSection dependsOn="overrideDescriptorFile" showOn=true titleKey="Override descriptor"]

            [@ww.textfield name="subject" labelKey="artifactory.task.pushToBintray.subject"/]
            [@ww.textfield name="repository" labelKey="artifactory.task.pushToBintray.repository"/]
            [@ww.textfield name="packageName" labelKey="artifactory.task.pushToBintray.packageName"/]
            [@ww.textfield name="version" labelKey="artifactory.task.pushToBintray.version"/]
            [@ww.textfield name="licenses" labelKey="artifactory.task.pushToBintray.licenses"/]
            [@ww.textfield name="vcsUrl" labelKey="artifactory.task.pushToBintray.vcsUrl"/]

        [/@ui.bambooSection]
        [@ww.select name="signMethod" label="Sign method" list=signMethodList listKey='key' listValue='value'/]
        [@ww.textfield name="gpgPassphrase" labelKey= "GPG Passphrase"/]
    [/@ww.form]
</div>
[#else]
    [@ui.header pageKey='Push to Bintray Log' /]

<div id="pushToBintrayLog" class="log"></div>

<script type="text/javascript">
    (function () {

        var logInterval;

        function updateLog() {
            AJS.$.ajax({
                url: '${req.contextPath}/getPushToBintrayLog.action?buildKey=${buildKey}&buildNumber=${buildNumber}',
                dataType: 'html',
                cache: false,
                success: function (html) {
                    var result = AJS.$(html);
                    if (result.filter('#pushDone').length) {
                        clearInterval(logInterval);
                    }

                    AJS.$('#pushToBintrayLog').html(result.filter('#pushLog').html());
                }
            });
        }

        updateLog();
        logInterval = setInterval(updateLog, 1000);
    })();
</script>
[/#if]
</body>
</html>