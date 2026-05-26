[@ui.bambooSection titleKey='artifactory.task.jfrogCli.deployment.title']

    [@ww.select
        name='artifactory.jfrogCli.serverId'
        labelKey='artifactory.task.jfrogCli.deployment.server'
        list=serverConfigManager.allServerConfigs
        listKey='id'
        listValue='url'
        emptyOption=true
        toggle='true'
    /]

    <div id="jfrogCliCommandDiv">
        [@ww.textarea
            name='artifactory.jfrogCli.command'
            labelKey='artifactory.task.jfrogCli.deployment.command'
            descriptionKey='artifactory.task.jfrogCli.deployment.command.description'
            rows='4'
            cols='80'
            cssClass='long-field'
        /]
    </div>

[/@ui.bambooSection]
