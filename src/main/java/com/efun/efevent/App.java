package com.efun.efevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 
 * @author Ken
 *
 */
@SpringBootApplication
@EnableAsync
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
