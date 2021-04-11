package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;

@Mojo(name = "property")
public class AwsPropertyMojo extends
        AbstractDeployAwsMojo<AmazonIdentityManagementClientBuilder, AmazonIdentityManagement> {

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    public AwsPropertyMojo() {
        super(AmazonIdentityManagementClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonIdentityManagement identityManagementClient) {
        String accountId = identityManagementClient.getUser().getUser().getUserId();
        project.getProperties().setProperty("aws.account.id", accountId);
        project.getProperties().setProperty("aws.account.id.lowercase", accountId.toLowerCase());
        getLog().info("The following properties have been set for the project");
        getLog().info("aws.account.id=" + accountId);
        getLog().info("aws.account.id.lowercase=" + accountId.toLowerCase());
    }

}
