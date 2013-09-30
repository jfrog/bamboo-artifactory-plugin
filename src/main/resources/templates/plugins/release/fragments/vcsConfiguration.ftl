<h2>
    VCS Configuration
</h2>
[#if git]
    [#if useShallowClone]
    <span style="font-weight: bold; color: red">WARNING: Cannot use release plugin with shallow clones. Please disable the use of shallow clones. </span>
    [/#if]
    [@ww.checkbox labelKey='artifactory.vcs.useReleaseBranch' name='useReleaseBranch' toggle='true'/]
[#--[@ui.bambooSection dependsOn='useReleaseBranch' showOn=true]--]
    [@ww.textfield labelKey='artifactory.vcs.releaseBranch' name='releaseBranch'/]
[#--[/@ui.bambooSection]--]
[/#if]
[@ww.checkbox labelKey='artifactory.vcs.createVcsTag' name='createVcsTag' toggle='true' value='true'/]
[#--TODO: RE-ENABLE SECTION WHEN DEVING FOR 2.4--]
[#--[@ui.bambooSection dependsOn='createVcsTag' showOn=true]--]
[@ww.textfield labelKey='artifactory.vcs.tagUrl' name='tagUrl'/]
[@ww.textarea labelKey='artifactory.vcs.tagComment' name='tagComment' rows='4' /]
[#--[/@ui.bambooSection]--]
[@ww.textarea labelKey='artifactory.vcs.nextDevelopmentComment' name='nextDevelopmentComment' rows='4' /]
<div id="artifactoryConfigDiv">
[@ww.select name='releasePublishingRepo' labelKey='artifactory.vcs.releasePublishingRepo' list='publishingRepos' toggle='true'/]
[@ww.textfield labelKey='artifactory.vcs.stagingComment' name='stagingComment'/]
</div>