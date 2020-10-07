<html>
<head>
    <meta name="tab" content="View Artifactory Build Info Result Summary"/>
    <meta name="decorator" content="result"/>
</head>

<body>

[#if !publishedBuildsDetails.isEmpty()]
    <div class="section">
        <h1> Artifactory Build Infos </h1>
        <table style="margin-top: 1em; margin-left:1em;">
        [@ui.bambooInfoDisplay]
            [#list publishedBuildsDetails as build]
                <tr>
                    <td>
                        <img style= "width: 48px; height: 48px; margin-right: 1em;" src="${baseUrl}/download/resources/${groupId}.${artifactId}/artifactory-icon.png"/>
                    </td>
                    <td style="vertical-align:middle">
                        <a href="${build.buildUrl}" target="_blank">${build.buildName}/${build.buildNumber}</a>
                    </td>
                </tr>
                <tr>
                    <td>
                        <br>
                    </td>
                </tr>
            [/#list]
        [/@ui.bambooInfoDisplay]
        </table>
    </div>
[/#if]

</body>
</html>