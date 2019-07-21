[@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
    [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryIvyBuilder.licenseViolationRecipients'/]

    [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryIvyBuilder.limitChecksToScopes'/]

    [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryIvyBuilder.includePublishedArtifacts' toggle='true'/]

    [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryIvyBuilder.disableAutoLicenseDiscovery' toggle='true'/]
[/@ui.bambooSection]
