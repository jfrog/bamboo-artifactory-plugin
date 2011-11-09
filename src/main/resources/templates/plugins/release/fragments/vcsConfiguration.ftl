<h2>
    VCS Configuration
</h2>
[#if git]
    [#if useShallowClone]
    <span style="font-weight: bold; color: red">WARNING: Cannot use release plugin with shallow clones. Please disable the use of shallow clones. </span>
    [/#if]
[@ww.checkbox labelKey='Use Release Branch: ' name='useReleaseBranch' toggle='true'/]
[@ui.bambooSection dependsOn='useReleaseBranch' showOn='true']
[@ww.textfield labelKey='Release branch: ' name='releaseBranch'/]
[/@ui.bambooSection]
[/#if]
[@ww.checkbox labelKey='Create VCS Tag' name='createVcsTag' toggle='true'/]
[@ui.bambooSection dependsOn='createVcsTag' showOn='true']
[@ww.textfield labelKey='Tag URL/name: ' name='tagUrl'/]
[@ww.textarea labelKey='Tag comment' name='tagComment' rows='4' /]
[/@ui.bambooSection]
[@ww.textarea labelKey='Next development version comment' name='nextDevelopmentComment' rows='4' /]
<div id="artifactoryConfigDiv">
[@ww.select name='releasePublishingRepo' labelKey='Publishing Repository' list='publishingRepos'
toggle='true' descriptionKey='Select a publishing repository.'/]
[@ww.textfield labelKey='Staging Comment: ' name='stagingComment'/]
</div>