package com.example.Social_Media_RestApi_Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SocialMediaProject {
	public static void main(String[] args) {
		SpringApplication.run(SocialMediaProject.class, args);
	}
}
