package com.workoutsmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkoutSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkoutSmartApplication.class, args);
    }
}
