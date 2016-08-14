<title>[@ww.text name="artifactoryServer.delete" /]</title>
    [@ww.form action="deleteServer" namespace="/admin"
submitLabelKey="global.buttons.delete"
id="confirmDelete"]
    [@s.hidden name="returnUrl" /]
    [@s.hidden name="serverId" /]
    [@ui.messageBox type="warning" titleKey="artifactory.server.delete.confirm.title" /]

<br/>

[/@ww.form]
