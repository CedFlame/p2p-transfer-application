package ru.shmatov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication
public class ProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class);
    }

}
