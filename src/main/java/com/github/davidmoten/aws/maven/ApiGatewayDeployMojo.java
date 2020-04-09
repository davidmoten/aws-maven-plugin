package com.github.davidmoten.aws.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployRestApi")
public final class ApiGatewayDeployMojo extends AbstractAwsMojo {

    @Parameter(property = "stage")
    private String stage;

    @Parameter(property = "restApiName")
    private String restApiName;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        ApiGatewayDeployer deployer = new ApiGatewayDeployer(getLog());
        deployer.deploy(credentials, region, restApiName, stage, proxy);
    }

}
