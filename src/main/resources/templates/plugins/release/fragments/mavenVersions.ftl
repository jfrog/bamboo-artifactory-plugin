[#assign versionsList= versions/]
[@ww.radio labelKey='artifactory.maven.moduleVersionConfiguration' name='moduleVersionConfiguration'
listKey='key' listValue='value' toggle='true' list=moduleVersionTypes ]
[/@ww.radio]
[@ui.bambooSection dependsOn='moduleVersionConfiguration' showOn='oneVersionAllModules']
[@ww.textfield labelKey='artifactory.maven.releaseValue' name='version.releaseValue' value = '${releaseValue}'/]
[@ww.textfield labelKey='artifactory.maven.nextIntegrationValue' name='version.nextIntegValue' value='${nextIntegValue}' /]
[/@ui.bambooSection]
[@ui.bambooSection dependsOn='moduleVersionConfiguration' showOn='versionPerModule']
    [#assign versionsList= versions/]
    [#if versionsList?size gt 0]
        [#assign showStatusIcon=true /]
        [#assign showOperations=true /]
        [#assign showAgent=false /]
        [#assign index = 0]
        [#list versionsList as version]
        <span class="brmp_versionrow">[@ww.textfield labelKey='artifactory.maven.moduleId' name = 'version.key' value='${version.key}' readonly='true'/] </span>
        [@ww.textfield labelKey='artifactory.maven.currentValue' name = 'version.currentValue' readonly='true' value='${version.originalValue}'/]
        [@ww.textfield labelKey='artifactory.maven.releaseValue' name = 'version.releaseValue' value='${version.releaseValue}'/]
            [#if version.releaseProp]
            [@ww.hidden name = 'version.nextIntegValue' value='${version.originalValue}'/]
                [#else]
                [@ww.textfield labelKey='artifactory.maven.nextIntegrationValue' name = 'version.nextIntegValue' value='${version.nextIntegValue}'/]
            [/#if]
        <br/>
        [/#list]
        [#else]
        <p class="marginned">
            <span style="font-weight: bold; color: red">WARNING: There are no Maven versions available for ${plan.name}
                . </span>
        </p>
    [/#if]
[/@ui.bambooSection]

