plugins {
	java
	id("org.springframework.boot") version "3.5.14"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.canonical"
version = "0.0.1-SNAPSHOT"
description = "Copyright agent based on Spring AI"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("org.springframework.ai:spring-ai-bom:1.1.6"))
	implementation(platform("org.springframework.shell:spring-shell-dependencies:4.0.2"))
	implementation("org.springframework.ai:spring-ai-starter-model-openai")
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	implementation("org.springframework.shell:spring-shell-starter-jna")
	implementation("org.apache.tika:tika-core:2.9.1")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.shell:spring-shell-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
