package com.example.novamart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.novamart.mapper")
public class NovaMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaMartApplication.class, args);
    }

}
