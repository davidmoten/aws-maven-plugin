package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployRestApi")
public final class ApiGatewayDeployMojo extends AbstractAwsMojo {

    @Parameter(property = "stage")
    private String stage;

    @Parameter(property = "restApiName")
    private String restApiName;

    @Override
    protected void execute(AwsKeyPair keyPair, String region, Proxy proxy) {
        ApiGatewayDeployer deployer = new ApiGatewayDeployer(getLog());
        deployer.deploy(keyPair, region, restApiName, stage, proxy);
    }

}
