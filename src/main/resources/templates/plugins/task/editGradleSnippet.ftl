[@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
    [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryGradleBuilder.licenseViolationRecipients'/]

    [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryGradleBuilder.limitChecksToScopes'/]

    [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryGradleBuilder.includePublishedArtifacts' toggle='true'/]

    [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryGradleBuilder.disableAutoLicenseDiscovery' toggle='true'/]
[/@ui.bambooSection]
