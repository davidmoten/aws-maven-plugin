package com.github.davidmoten.aws.maven;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "property")
public class AwsPropertyMojo extends AbstractAwsMojo {

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Override
    protected void execute(AwsKeyPair keyPair, String region, Proxy proxy) {
        AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));
        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder //
                .standard() //
                .withRegion(region) //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .build();
        String accountId = iam.getUser().getUser().getUserId();
        project.getProperties().setProperty("aws.account.id", accountId);
        getLog().info("The following properties have been set for the project");
        getLog().info("aws.account.id=" + accountId);
    }

}
