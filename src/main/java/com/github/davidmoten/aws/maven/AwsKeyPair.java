package com.github.davidmoten.aws.maven;

final class AwsKeyPair {
    final String key;
    final String secret;

    AwsKeyPair(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }
}
