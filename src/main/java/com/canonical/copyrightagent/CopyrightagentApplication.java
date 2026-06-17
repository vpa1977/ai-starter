package com.canonical.copyrightagent;

import org.debian.decopy.Options;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CopyrightagentApplication {

	public static void main(String[] args) {
		Options options = Options.parse(args);

		SpringApplication.run(CopyrightagentApplication.class, args);
	}

}
