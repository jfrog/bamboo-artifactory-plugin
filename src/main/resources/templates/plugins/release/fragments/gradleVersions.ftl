[#assign versionsList= versions/]

[@ww.checkbox labelKey = "Use existing version." value=false name="moduleVersionConfiguration" toggle='true'/]
<br/>
[@ui.bambooSection dependsOn = "moduleVersionConfiguration" showOn = false]

    [#if versionsList?size gt 0]
        [#assign showStatusIcon=true /]
        [#assign showOperations=true /]
        [#assign showAgent=false /]
        [#assign index = 0]
        [#list versionsList as version]

        <span class="brmp_versionrow">
            [@ww.textfield labelKey='artifactory.gradle.properyKey' name = 'version.key' value='${version.key}' readonly='true'/] </span>
            [@ww.textfield labelKey='artifactory.gradle.currentValue' name = 'version.currentValue' readonly='true' value='${version.originalValue}'/]
            [@ww.textfield labelKey='artifactory.gradle.releaseValue' name='version.releaseValue' value = '${version.releaseValue}'/]
            [#if version.releaseProp]
                [@ww.hidden name = 'version.nextIntegValue' value='${version.originalValue}'/]
            [#else]
                [@ww.textfield labelKey='artifactory.gradle.nextIntegrationValue' name = 'version.nextIntegValue' value='${version.nextIntegValue}'/]
            [/#if]

        <br/>
        [/#list]

    [#else]
    <p class="marginned">
    <span style="font-weight: bold; color: red">WARNING: No version property found in gradle.properties file based on the release Property Key provided for ${plan.name}
        .</span>
    </p>
    [/#if]
[/@ui.bambooSection]
<script>
    var script = document.createElement('script');
    script.innerHTML = "document.querySelector('.dependsOnmoduleVersionConfiguration').style.display=''";
    document.body.lastChild.appendChild(script);
</script>
