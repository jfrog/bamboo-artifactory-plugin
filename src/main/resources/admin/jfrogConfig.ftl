[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.JfrogConfigAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.JfrogConfigAction" --]
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

[@dj.tabContainer headingKeys=["artifactory.server.tab.title"] selectedTab='${"artifactory.server.tab.title"}']
    [@dj.contentPane labelKey="artifactory.server.tab.title"]
        [@manageServersTab/]
    [/@dj.contentPane]
[/@dj.tabContainer]
</body>
</html>


[#macro manageServersTab]
    [@ww.action name="existingArtifactoryServer" executeResult="true"/]
[/#macro]
