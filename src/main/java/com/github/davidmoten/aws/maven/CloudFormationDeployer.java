package com.github.davidmoten.aws.maven;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;

final class CloudFormationDeployer {

    public void deploy(AwsKeyPair keyPair, String region, String inputDirectory, final String stackName,
            final String templateBody, Map<String, String> parameters, Proxy proxy) {

        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AmazonCloudFormation cf = AmazonCloudFormationClientBuilder //
                .standard() //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();

        Parameter params = new Parameter();
        for (Entry<String, String> entry : parameters.entrySet()) {
            params = params.withParameterKey(entry.getKey()) //
                    .withParameterValue(entry.getValue());
        }

        ListStacksResult r = cf.listStacks();
        boolean exists = r.getStackSummaries() //
                .stream() //
                .anyMatch(x -> x.getStackName().equals(stackName));
        if (!exists) {
            cf.createStack(new CreateStackRequest() //
                    .withStackName(stackName) //
                    .withTemplateBody(templateBody) //
                    .withParameters(params));
        } else {
            cf.updateStack(new UpdateStackRequest() //
                    .withStackName(stackName) //
                    .withTemplateBody(templateBody) //
                    .withParameters(params));
        }

        // TODO wait

    }

    // Wait for a stack to complete transitioning
    // End stack states are:
    // CREATE_COMPLETE
    // CREATE_FAILED
    // DELETE_FAILED
    // ROLLBACK_FAILED
    // OR the stack no longer exists
    public static String waitForCompletion(AmazonCloudFormation stackbuilder, String stackName) throws Exception {

        DescribeStacksRequest wait = new DescribeStacksRequest();
        wait.setStackName(stackName);
        Boolean completed = false;
        String stackStatus = "Unknown";
        String stackReason = "";

        System.out.print("Waiting");

        while (!completed) {
            List<Stack> stacks = stackbuilder.describeStacks(wait).getStacks();
            if (stacks.isEmpty()) {
                completed = true;
                stackStatus = "NO_SUCH_STACK";
                stackReason = "Stack has been deleted";
            } else {
                for (Stack stack : stacks) {
                    if (stack.getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString())
                            || stack.getStackStatus().equals(StackStatus.CREATE_FAILED.toString())
                            || stack.getStackStatus().equals(StackStatus.ROLLBACK_FAILED.toString())
                            || stack.getStackStatus().equals(StackStatus.DELETE_FAILED.toString())) {
                        completed = true;
                        stackStatus = stack.getStackStatus();
                        stackReason = stack.getStackStatusReason();
                    }
                }
            }

            // Show we are waiting
            System.out.print(".");

            // Not done yet so sleep for 10 seconds.
            if (!completed)
                Thread.sleep(10000);
        }

        // Show we are done
        System.out.print("done\n");

        return stackStatus + " (" + stackReason + ")";
    }

}
