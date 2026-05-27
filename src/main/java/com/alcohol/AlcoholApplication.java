package com.alcohol;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** BarLog / 余味 后端启动入口。 */
@SpringBootApplication
@MapperScan("com.alcohol.mapper")
@EnableTransactionManagement
public class AlcoholApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlcoholApplication.class, args);
    }
}
