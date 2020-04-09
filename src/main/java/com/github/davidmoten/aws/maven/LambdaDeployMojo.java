package com.github.davidmoten.aws.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
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
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        LambdaDeployer deployer = new LambdaDeployer(getLog());
        deployer.deploy(credentials, region, artifact, functionName, functionAlias, proxy);
    }

}
