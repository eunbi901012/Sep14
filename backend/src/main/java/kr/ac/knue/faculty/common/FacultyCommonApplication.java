package kr.ac.knue.faculty.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("kr.ac.knue.faculty.common.mapper")
public class FacultyCommonApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacultyCommonApplication.class, args);
    }
}
