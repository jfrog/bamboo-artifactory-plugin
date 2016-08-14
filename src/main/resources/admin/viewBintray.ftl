[#-- @ftlvariable name="action" type="org.jfrog.bamboo.admin.ConfigureBintrayAction" --]
[#-- @ftlvariable name="" type="org.jfrog.bamboo.admin.ConfigureBintrayAction" --]
<div class="toolbar">
    <div class="aui-toolbar inline">
        <ul class="toolbar-group">
            <li class="toolbar-item">
                <a class="toolbar-trigger"
                   href="[@s.url action='configureBintray.action' namespace='/admin' /]">
                [@s.text name='bintray.set.config' /]</a>
            </li>
        </ul>
    </div>
</div>

[@ui.header pageKey="bintray.config.heading" descriptionKey="bintray.config.description"/]
