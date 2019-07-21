    [#--Env integration--]
    [@ww.checkbox labelKey='artifactory.task.includeEnvVars' name='artifactory.generic.includeEnvVars' toggle='true'/]

    [@ui.bambooSection dependsOn='artifactory.generic.includeEnvVars' showOn=true]
        [@ww.textfield labelKey='artifactory.task.envVarsIncludePatterns' name='artifactory.generic.envVarsIncludePatterns' /]
        [@ww.textfield labelKey='artifactory.task.envVarsExcludePatterns' name='artifactory.generic.envVarsExcludePatterns' /]
    [/@ui.bambooSection]
