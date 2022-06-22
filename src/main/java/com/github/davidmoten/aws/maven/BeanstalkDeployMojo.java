package com.github.davidmoten.aws.maven;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupRulesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Mojo(name = "deploy")
public final class BeanstalkDeployMojo extends AbstractAwsMojo {

    @Parameter(property = "applicationName")
    private String applicationName;

    @Parameter(property = "environmentName")
    private String environmentName;

    @Parameter(property = "artifact")
    private File artifact;

    @Parameter(property = "versionLabel")
    private String versionLabel;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "portsToRemove")
    private List<String> portsToRemoveValues;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        if (versionLabel == null) {
            versionLabel = createVersionLabel(applicationName, new Date(), project.getVersion());
        }
        ClientConfiguration clientConfiguration = Util.createConfiguration(proxy);
        AWSElasticBeanstalk beanstalk = AWSElasticBeanstalkClientBuilder.standard().withRegion(region)
                .withCredentials(credentials).withClientConfiguration(clientConfiguration).build();
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).withCredentials(credentials)
                .withClientConfiguration(clientConfiguration).build();
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(credentials)
                .withClientConfiguration(clientConfiguration).build();
        BeanstalkDeployer deployer = new BeanstalkDeployer(getLog(), beanstalk, s3);
        deployer.deploy(artifact, applicationName, environmentName, versionLabel);

        Set<Integer> portsToRemove = portsToRemoveValues == null ? Collections.emptySet()
                : portsToRemoveValues.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet());

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

            getLog().info("getting security group ids for instance ids " + instanceIds);
            List<String> securityGroupIds = ec2
                    .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds)) //
                    .getReservations() //
                    .stream() //
                    .flatMap(y -> y.getGroups().stream().map(z -> z.getGroupId())) //
                    .collect(Collectors.toList());

            getLog().info("getting security group rules for security group ids " + securityGroupIds);
            Filter filter = new Filter();
            filter.setName("group-id");
            filter.setValues(securityGroupIds);
            List<String> securityGroupRuleIds = ec2
                    .describeSecurityGroupRules(new DescribeSecurityGroupRulesRequest().withFilters(filter)) //
                    .getSecurityGroupRules() //
                    .stream() //
                    .filter(x -> portsToRemove.contains(x.getToPort())) //
                    .map(x -> x.getSecurityGroupRuleId()) //
                    .collect(Collectors.toList());

            getLog().info("removing security group rules " + securityGroupRuleIds);
            ec2.revokeSecurityGroupIngress(
                    new RevokeSecurityGroupIngressRequest().withSecurityGroupRuleIds(securityGroupRuleIds));
        }
    }

    private static String createVersionLabel(String applicationName, Date date, String version) {
        // construct version label using application name and dateTime
        return applicationName + "_" + version + "_" + Util.formatDateTime(date);
    }

}
