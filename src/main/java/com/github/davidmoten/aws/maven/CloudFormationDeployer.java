package com.github.davidmoten.aws.maven;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.logging.Log;

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

    private final Log log;

    CloudFormationDeployer(Log log) {
        this.log = log;
    }

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

        Status result = waitForCompletion(cf, stackName);
        log.info(result.toString());
        if (!result.value.equals(StackStatus.CREATE_COMPLETE) //
                && !result.value.equals(StackStatus.UPDATE_COMPLETE)) {
            throw new RuntimeException("create/update failed: " + result);
        }
    }

    private static class Status {
        final String value;
        final String reason;

        Status(String value, String reason) {
            this.value = value;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "Status [value=" + value + ", reason=" + reason + "]";
        }

    }

    // Wait for a stack to complete transition
    // End stack states are:
    // CREATE_COMPLETE
    // CREATE_FAILED
    // DELETE_FAILED
    // ROLLBACK_FAILED
    // OR the stack no longer exists
    public static Status waitForCompletion(AmazonCloudFormation stackbuilder, String stackName) {

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
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }

        // Show we are done
        System.out.print("done\n");

        return new Status(stackStatus, stackReason);
    }

}
