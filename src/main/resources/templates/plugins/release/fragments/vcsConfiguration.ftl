<h2>
    VCS Configuration
</h2>
[#if git]
    [#if useShallowClone]
    <span style="font-weight: bold; color: red">WARNING: Cannot use release plugin with shallow clones. Please disable the use of shallow clones. </span>
    [/#if]
    [@ww.checkbox labelKey='artifactory.vcs.useReleaseBranch' name='useReleaseBranch' toggle='true'  onchange='javascript: hideOrShow(this)'/]
[#--[@ui.bambooSection dependsOn='useReleaseBranch' showOn=true]--]
    [@ww.textfield labelKey='artifactory.vcs.releaseBranch' name='releaseBranch'/]
[#--[/@ui.bambooSection]--]
[/#if]
[@ww.checkbox labelKey='artifactory.vcs.createVcsTag' name='createVcsTag' toggle='true' value='true' onchange='javascript: hideOrShow(this)'/]
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
<script>
    function hideOrShow(cb) {
        var name = cb.name;
        if(name == "createVcsTag"){
            hideOrShowVCSTag(cb);
        }else{
            hideOrShowReleaseBranch(cb);
        }
    }

    function hideOrShowReleaseBranch(cb){
        var releaseBranch = document.getElementById('fieldArea_releaseBuild_releaseBranch');
        if (!cb.checked) {
            releaseBranch.style.display = "none";
        }else{
            releaseBranch.style.display = "inherit";
        }
    }

    function hideOrShowVCSTag(cb){
        var tagURL = document.getElementById('fieldArea_releaseBuild_tagUrl');
        var tagComment = document.getElementById('fieldArea_releaseBuild_tagComment');
        var nextComment = document.getElementById('fieldArea_releaseBuild_nextDevelopmentComment');
        if (!cb.checked) {
            tagURL.style.display = "none";
            tagComment.style.display = "none";
            nextComment.style.display = "none";
        }else{
            tagURL.style.display = "inherit";
            tagComment.style.display = "inherit";
            nextComment.style.display = "inherit";

        }
    }

</script>