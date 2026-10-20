package com.webizera.schemadrift;

import org.springframework.boot.SpringApplication;

public class TestSchemaDriftApplication {

    public static void main(String[] args) {
        SpringApplication.from(SchemaDriftApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
