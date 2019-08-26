package com.github.davidmoten.aws.maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class LambdaDeployerTest {

    @Test
    public void testDashReplacedWithUnderscore() {
        assertEquals("a_b_c", LambdaDeployer.sanitizeFunctionAlias("a-b-c"));
    }

    @Test
    public void testDotReplacedWithUnderscore() {
        assertEquals("a_b_c", LambdaDeployer.sanitizeFunctionAlias("a.b.c"));
    }

    @Test
    public void testColonsRemoved() {
        assertEquals("abc", LambdaDeployer.sanitizeFunctionAlias("a:b:c"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullThrowsNPE() {
        LambdaDeployer.sanitizeFunctionAlias(null);
    }

}
