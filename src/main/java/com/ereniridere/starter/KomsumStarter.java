package com.ereniridere.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.ereniridere")
@ComponentScan(basePackages = "com.ereniridere")
@EntityScan(basePackages = "com.ereniridere")
@SpringBootApplication
public class KomsumStarter {

	public static void main(String[] args) {
		SpringApplication.run(KomsumStarter.class, args);
	}

}
