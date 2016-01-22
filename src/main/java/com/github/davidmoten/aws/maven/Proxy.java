package com.github.davidmoten.aws.maven;

class Proxy {

    final String host;
    final int port;
    final String username;
    final String password;

    Proxy(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}