[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.ManageArtifactoryServersAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.ManageArtifactoryServersAction" --]
<html>
<head>
    <title>Manage Artifactory Plugin Configuration</title>
    <meta name="decorator" content="adminpage">
</head>

<body>
<div>
[@ui.header pageKey=i18n.getText('artifactory.server.manage.title') /]
<br/>
[#if action.isMissedMigration()]
    [@ui.messageBox type="warning"]
        [@ww.text name=i18n.getText("Artifactory plugin data could not be found, for more info please refer JFrog's wiki.")]
        [/@ww.text]
    [/@ui.messageBox]
[/#if]
</div>

[@dj.tabContainer headingKeys=["artifactory.server.tab.title", "bintray.config.tab.title"] selectedTab='${"artifactory.server.tab.title"}']
    [@dj.contentPane labelKey="artifactory.server.tab.title"]
        [@manageServersTab/]
    [/@dj.contentPane]
    [@dj.contentPane labelKey="bintray.config.tab.title"]
        [@managebintrayTab/]
    [/@dj.contentPane]
[/@dj.tabContainer]

<script type="text/javascript">
    AJS.$(function ()
    {
        var str = window.location.pathname+window.location.search;
        if (str.includes("/admin/saveBintrayConf.action")) {
            document.getElementById("aui-uid-1").click()
        }
    });
</script>
</body>
</html>


[#macro manageServersTab]
    [@ww.action name="existingArtifactoryServer" executeResult="true"/]
[/#macro]


[#macro managebintrayTab]
    [@ww.action name="configureBintray" executeResult="true"/]
[/#macro]