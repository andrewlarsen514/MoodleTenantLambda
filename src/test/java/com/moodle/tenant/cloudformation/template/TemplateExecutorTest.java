package com.moodle.tenant.cloudformation.template;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.*;
import com.moodle.tenant.cloudformation.MoodleTemplate;
import com.moodle.tenant.cloudformation.Template;
import com.moodle.tenant.cloudformation.TemplateExecutor;
import com.moodle.tenant.exception.MoodleStackException;
import com.moodle.tenant.model.LoadBalancer;
import com.moodle.tenant.model.MoodleTenant;
import com.moodle.tenant.model.StackRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Created by andrewlarsen on 8/27/17.
 */
public class TemplateExecutorTest {
    private TemplateExecutor executor;
    private Template template;
    private MoodleTenant tenant;
    @Mock
    AmazonCloudFormation amazonCloudFormation;

    private String stackId = "STACKID";
    private String body = "BODY";
    private String stackName = "STACKNAME";
    @Before
    public void setup(){
        tenant = new MoodleTenant();
        tenant.setClientName("CLIENT");
        tenant.setEcsCluser("CLUSTER");
        tenant.setHostedZoneName("HOSTEDZONE");
        tenant.setPriority(1);
        tenant.setVpcId("VPCID");
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setArn("ARN");
        loadBalancer.setDnsName("DSNAME");
        loadBalancer.setHostedZoneName("HOZEDZONE");
        loadBalancer.setListener("LISTENER");
        tenant.setLoadBalancer(loadBalancer);

        MockitoAnnotations.initMocks(this);
        executor = new TemplateExecutor(amazonCloudFormation);
        template = new MoodleTemplate(Optional.of(new ArrayList<>()), "", new StackRequest());
        template.setStackName("SOMENAME");
    }
    @Test
    public void buildStack() throws Exception {
        given(amazonCloudFormation.createStack(any())).willReturn(new CreateStackResult()
                .withStackId(stackId));
        given(amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackId)))
                .willReturn(new DescribeStacksResult().withStacks(new Stack().withStackStatus(StackStatus.CREATE_COMPLETE)
                        .withStackId(stackId)));

        Stack stack = executor.buildStack(Optional.of(template));
        assertThat(stack.getStackId(), is(stackId));

    }

    @Test(expected = MoodleStackException.class)
    public void buildStackRollBackException() throws Exception {

        given(amazonCloudFormation.createStack(any())).willReturn(new CreateStackResult()
                .withStackId(stackId));

        given(amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackId)))
                .willReturn(new DescribeStacksResult().withStacks(new Stack().withStackStatus(StackStatus.ROLLBACK_FAILED)
                        .withStackId(stackId)));

        Stack stack = executor.buildStack(Optional.of(template));

        assertThat(stack.getStackId(), is(stackId));

    }
    @Test(expected = MoodleStackException.class)
    public void buildStackInProgressThenException() throws Exception {

        given(amazonCloudFormation.createStack(any())).willReturn(new CreateStackResult()
                .withStackId(stackId));
        given(amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackId)))
                .willReturn(new DescribeStacksResult().withStacks(new Stack().withStackStatus(StackStatus.CREATE_IN_PROGRESS)
                        .withStackId(stackId)));

        given(amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackId)))
                .willReturn(new DescribeStacksResult().withStacks(new Stack().withStackStatus(StackStatus.CREATE_FAILED)
                        .withStackId(stackId)));

        Stack stack = executor.buildStack(Optional.of(template));

        assertThat(stack.getStackId(), is(stackId));

    }
    @Test(expected = IllegalArgumentException.class)
    public void buildStackRollNotemplateThrowsException() throws Exception {

        Stack stack = executor.buildStack(Optional.empty());

        assertThat(stack.getStackId(), is(stackId));

    }

}