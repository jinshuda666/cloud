package com.jinshuda.cloudlibrarybackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.jinshuda.cloudlibrarybackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class CloudLibraryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudLibraryBackendApplication.class, args);
    }

}
