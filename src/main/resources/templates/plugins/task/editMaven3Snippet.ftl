[@ui.bambooSection dependsOn='runLicenseChecks' showOn=true]
    [@ww.textfield labelKey='artifactory.task.licenseViolationRecipients' name='builder.artifactoryMaven3Builder.licenseViolationRecipients' /]

    [@ww.textfield labelKey='artifactory.task.limitChecksToScopes' name='builder.artifactoryMaven3Builder.limitChecksToScopes' /]

    [@ww.checkbox labelKey='artifactory.task.includePublishedArtifacts' name='builder.artifactoryMaven3Builder.includePublishedArtifacts' toggle='true'/]

    [@ww.checkbox labelKey='artifactory.task.disableAutoLicenseDiscovery' name='builder.artifactoryMaven3Builder.disableAutoLicenseDiscovery' toggle='true'/]
[/@ui.bambooSection]

[@ww.checkbox labelKey='artifactory.task.maven.recordAllDependencies' name='recordAllDependencies' toggle='true' /]
