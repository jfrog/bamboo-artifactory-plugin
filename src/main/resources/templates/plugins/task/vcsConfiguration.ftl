[@s.select
labelKey=i18n.getText('artifactory.vcs.type')
name='artifactory.vcs.type'
toggle=true
list="artifactory.vcs.git.vcs.type.list"
listKey='name'
listValue='label']
[/@s.select]

[@ui.bambooSection dependsOn='artifactory.vcs.type' showOn='GIT']

    [@ww.textfield labelKey=i18n.getText('artifactory.vcs.git.url') name='artifactory.vcs.git.url' required='true'/]

    [@s.select
    labelKey=i18n.getText('artifactory.vcs.git.authenticationType')
    name='artifactory.vcs.git.authenticationType'
    toggle=true
    list="artifactory.vcs.git.authenticationType.list"
    listKey='name'
    listValue='label']
    [/@s.select]

    [@ui.bambooSection dependsOn='artifactory.vcs.git.authenticationType' showOn='PASSWORD']
        [@ww.textfield labelKey=i18n.getText('artifactory.vcs.git.username') name='artifactory.vcs.git.username' required='true'/]
        [@ww.password labelKey=i18n.getText('artifactory.vcs.git.password') name='artifactory.vcs.git.password' showPassword='true' required='true'/]
            [#--The Dummy password is a workaround for the autofill (Chrome)--]
        [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
    [/@ui.bambooSection]

    [@ui.bambooSection dependsOn='artifactory.vcs.git.authenticationType' showOn='SSH_KEYPAIR']
        [#if context.get('artifactory.vcs.git.ssh.key')?has_content]
            [@ww.checkbox labelKey=i18n.getText('artifactory.vcs.git.ssh.key.change') toggle='true' name='change_key'/]
            [@ui.bambooSection dependsOn='change_key' showOn=true]
                [@s.file labelKey=i18n.getText('artifactory.vcs.git.ssh.key') name='artifactory.vcs.git.ssh.key' required='true'/]
            [/@ui.bambooSection]
        [#else]
            [@s.file labelKey=i18n.getText('artifactory.vcs.git.ssh.key') name='artifactory.vcs.git.ssh.key' required='true'/]
        [/#if]
        [@s.password labelKey=i18n.getText('artifactory.vcs.git.ssh.passphrase') name='artifactory.vcs.git.ssh.passphrase' showPassword='true'/]
            [#--The Dummy password is a workaround for the autofill (Chrome)--]
        [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
    [/@ui.bambooSection]
[/@ui.bambooSection]
[@ui.bambooSection dependsOn='artifactory.vcs.type' showOn='PERFORCE']
    [@s.textfield labelKey=i18n.getText('artifactory.vcs.p4.port') name='artifactory.vcs.p4.port' required=true /]
    [@s.textfield labelKey=i18n.getText('artifactory.vcs.p4.client') name='artifactory.vcs.p4.client' required=true cssClass="long-field" /]
    [@s.textfield labelKey=i18n.getText('artifactory.vcs.p4.depot') name='artifactory.vcs.p4.depot' required=true cssClass="long-field" /]
    [@s.textfield labelKey=i18n.getText('artifactory.vcs.p4.username') name='artifactory.vcs.p4.username' /]
    [@s.password labelKey=i18n.getText('artifactory.vcs.p4.password') name='artifactory.vcs.p4.password' showPassword='true' required=false/]
        [#--The Dummy password is a workaround for the autofill (Chrome)--]
    [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
[/@ui.bambooSection]