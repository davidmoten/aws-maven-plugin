package com.github.davidmoten.aws.maven;

import java.util.Map;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.GetStageRequest;
import com.amazonaws.services.apigateway.model.GetStageResult;
import com.amazonaws.services.apigateway.model.MethodSnapshot;
import com.amazonaws.services.apigateway.model.UpdateDeploymentRequest;
import com.amazonaws.services.apigateway.model.UpdateDeploymentResult;

final class ApiGatewayDeployer {

    private final Log log;

    ApiGatewayDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, final String restApiId, final String stage, Proxy proxy) {
        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonApiGateway ag = AmazonApiGatewayClientBuilder.standard().withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();
        log.info("creating deployment of " + restApiId + ":" + stage);
        CreateDeploymentResult r = ag
                .createDeployment(new CreateDeploymentRequest().withRestApiId(restApiId).withStageName(stage));
        Map<String, Map<String, MethodSnapshot>> summary = r.getApiSummary();
        log.info("created deployment");
        log.info("summary=" + summary);
    }

}
