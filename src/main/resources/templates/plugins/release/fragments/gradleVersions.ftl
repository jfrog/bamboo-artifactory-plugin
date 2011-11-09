[#assign versionsList= versions/]
[#if versionsList?size gt 0]
    [#assign showStatusIcon=true /]
    [#assign showOperations=true /]
    [#assign showAgent=false /]
    [#assign index = 0]
    [#list versionsList as version]
    <span class="brmp_versionrow">[@ww.textfield labelKey='Property Key' name = 'version.key' value='${version.key}' readonly='true'/] </span>
    [@ww.textfield labelKey='Current Value' name = 'version.currentValue' readonly='true' value='${version.originalValue}'/]
    [@ww.textfield labelKey='Release Value' name = 'version.releaseValue' value='${version.releaseValue}'/]
        [#if version.releaseProp]
        [@ww.hidden name = 'version.nextIntegValue' value='${version.originalValue}'/]
            [#else]
            [@ww.textfield labelKey='Next Integration Value: ' name = 'version.nextIntegValue' value='${version.nextIntegValue}'/]
        [/#if]
    <br/>
    [/#list]
    [#include "vcsConfiguration.ftl"/]
    [#else]
    <p class="marginned">
        There are no Gradle versions available for ${plan.name}.
    </p>
[/#if]