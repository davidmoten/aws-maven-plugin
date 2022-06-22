package com.github.davidmoten.aws.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupRulesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressResult;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;

@Mojo(name = "removePorts")
public final class BeanstalkRemovePortsMojo extends AbstractAwsMojo {

    @Parameter(property = "applicationName")
    private String applicationName;

    @Parameter(property = "environmentName")
    private String environmentName;

    @Parameter(name = "removePorts")
    private List<String> removePorts;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {

        ClientConfiguration clientConfiguration = Util.createConfiguration(proxy);
        AWSElasticBeanstalk beanstalk = AWSElasticBeanstalkClientBuilder //
                .standard() //
                .withRegion(region).withCredentials(credentials) //
                .withClientConfiguration(clientConfiguration) //
                .build();
        AmazonEC2 ec2 = AmazonEC2ClientBuilder //
                .standard() //
                .withRegion(region) //
                .withCredentials(credentials) //
                .withClientConfiguration(clientConfiguration) //
                .build();

        // removal of ports logic exists because AWS Beanstalk has lousy support for
        // removing particular ports from cloudformation deployments. In particular
        // port 80 from Tomcat image single instance deployments is left open to
        // anywhere.
        Set<Integer> portsToRemove = removePorts == null ? Collections.emptySet()
                : removePorts.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet());

        if (portsToRemove != null && !portsToRemove.isEmpty()) {

            getLog().info("getting instance ids for environment " + environmentName);
            List<String> instanceIds = beanstalk
                    .describeEnvironmentResources(
                            new DescribeEnvironmentResourcesRequest().withEnvironmentName(environmentName)) //
                    .getEnvironmentResources() //
                    .getInstances() //
                    .stream() //
                    .map(x -> x.getId()) //
                    .collect(Collectors.toList());
            if (instanceIds.isEmpty()) {
                getLog().info("no instances found");
                return;
            }
            getLog().info("getting security group ids for instance ids " + instanceIds);
            List<String> securityGroupIds = ec2
                    .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds)) //
                    .getReservations() //
                    .stream() //
                    .flatMap(y -> y //
                            .getInstances() //
                            .stream() //
                            .flatMap(z -> z.getNetworkInterfaces().stream()) //
                            .flatMap(z -> z.getGroups().stream()).map(z -> z.getGroupId())) //
                    .collect(Collectors.toList());
            if (securityGroupIds.isEmpty()) {
                getLog().info("no security groups found");
                return;
            }
            getLog().info("getting security group rules for security group ids " + securityGroupIds);
            Filter filter = new Filter();
            filter.setName("group-id");
            filter.setValues(securityGroupIds);
            Map<String, List<String>> securityGroupRules = new HashMap<>();
            ec2.describeSecurityGroupRules(
                    new DescribeSecurityGroupRulesRequest().withFilters(filter).withMaxResults(1000)) //
                    .getSecurityGroupRules() //
                    .stream() //
                    .filter(x -> portsToRemove.contains(x.getToPort())) //
                    .forEach(x -> {
                        List<String> list = securityGroupRules.get(x.getGroupId());
                        if (list == null) {
                            list = new ArrayList<>();
                            securityGroupRules.put(x.getGroupId(), list);
                        }
                        list.add(x.getSecurityGroupRuleId());
                    });

            if (securityGroupRules.isEmpty()) {
                getLog().info("no security group rules found");
                return;
            }
            getLog().info("revoking security group rules " + securityGroupRules);
            securityGroupRules.forEach((groupId, ruleIds) -> {
                RevokeSecurityGroupIngressResult result = ec2.revokeSecurityGroupIngress(
                        new RevokeSecurityGroupIngressRequest().withGroupId(groupId).withSecurityGroupRuleIds(ruleIds));
                getLog().info("revoked=" + result.getReturn() + " for groupId=" + groupId + ", ruleIds=" + ruleIds);
            });
        }
    }

}
