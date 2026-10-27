package com.hhm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hhm", "com.controller", "com.service", "com.dao", "com.util", "com.graphql"})
public class HhmApplication
{
    public static void main(String[] args) {
       SpringApplication.run(HhmApplication.class, args);
    }
}
