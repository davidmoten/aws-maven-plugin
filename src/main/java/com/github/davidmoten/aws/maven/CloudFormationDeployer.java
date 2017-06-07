package com.github.davidmoten.aws.maven;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;

public class CloudFormationDeployer {

    public void deploy(AwsKeyPair keyPair, String region, String inputDirectory, final String stackName,
            final String templateBody, Proxy proxy) {

        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonCloudFormation cf = AmazonCloudFormationClientBuilder //
                .standard() //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();
        boolean created = cf
                .createStack(new CreateStackRequest() //
                        .withStackName(stackName) //
                        .withTemplateBody(templateBody)) //
                .getSdkHttpMetadata() //
                .getHttpStatusCode() == 200;
        if (!created) {
            boolean ok = cf
                    .updateStack(new UpdateStackRequest() //
                            .withStackName(stackName) //
                            .withTemplateBody(templateBody)) //
                    .getSdkHttpMetadata() //
                    .getHttpStatusCode() == 200;
        }
    }

}
