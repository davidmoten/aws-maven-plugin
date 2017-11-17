package com.github.davidmoten.aws.maven;

import java.util.Map;
import java.util.Optional;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.MethodSnapshot;
import com.amazonaws.services.apigateway.model.RestApi;

final class ApiGatewayDeployer {

    private final Log log;

    ApiGatewayDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, final String restApiName, final String stage, Proxy proxy) {
        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonApiGateway ag = AmazonApiGatewayClientBuilder.standard().withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();
        GetRestApisResult apis = ag.getRestApis(new GetRestApisRequest().withLimit(10000));
        Optional<RestApi> api = apis.getItems().stream().filter(item -> item.getName().equals(restApiName)).findFirst();
        RestApi a = api.orElseThrow(() -> new RuntimeException("no rest api found with name='" + restApiName + "'"));
        String restApiId = a.getId();
        log.info("creating deployment of " + restApiId + " to stage " + stage);
        CreateDeploymentResult r = ag
                .createDeployment(new CreateDeploymentRequest().withRestApiId(restApiId).withStageName(stage));
        Map<String, Map<String, MethodSnapshot>> summary = r.getApiSummary();
        log.info("created deployment");
        log.info("summary=" + summary);
    }

}
