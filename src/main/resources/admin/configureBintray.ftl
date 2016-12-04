<div>
        [@ww.form action='saveBintrayConf'
                  descriptionKey='bintray.config.description'
                  submitLabelKey='global.buttons.submit'
        ]
        <br/>
            [@ww.param name='buttons']
                [@ww.submit value=action.getText('global.buttons.test') name="sendTest" namespace="/admin" /]
            [/@ww.param]
            [@ui.bambooSection]
                [@ww.textfield labelKey='bintray.username' name="bintrayUsername" required="false" autofocus=true /]
                [@ww.password labelKey='bintray.apikey' name="bintrayApiKey" showPassword="true" required="false" /]
                [@ww.textfield labelKey='bintray.sonatype.username' name="sonatypeOssUsername" required="false" /]
                [@ww.password labelKey='bintray.sonatype.password' name="sonatypeOssPassword" showPassword="true" required="false" /]
                [#--The Dummy password is a workaround for the autofill (Chrome)--]
                [@ww.password name='artifactory.password.DUMMY' cssStyle='visibility:hidden; position: absolute'/]
        [/@ui.bambooSection]
        [/@ww.form]
</div>
