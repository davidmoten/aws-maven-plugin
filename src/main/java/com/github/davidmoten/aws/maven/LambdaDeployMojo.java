package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployLambda")
public final class LambdaDeployMojo extends AbstractAwsMojo {

    @Parameter(property = "functionName")
    private String functionName;

    @Parameter(property = "functionAlias")
    private String functionAlias;

    @Parameter(property = "artifact")
    private String artifact;

    @Override
    protected void execute(AwsKeyPair keyPair, String region, Proxy proxy) {
        LambdaDeployer deployer = new LambdaDeployer(getLog());
        deployer.deploy(keyPair, region, artifact, functionName, functionAlias, proxy);
    }

}
