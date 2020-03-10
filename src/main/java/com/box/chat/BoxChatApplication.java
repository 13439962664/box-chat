package com.box.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages= {"com.box.**"})
@EnableAsync
@ServletComponentScan
@MapperScan("com.box.**.dao")
public class BoxChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoxChatApplication.class, args);
	}

}
