package de.dddns.kirbylink.archiva.config;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Validated
@Configuration
@ConfigurationProperties(prefix = "apache-archiva.configuration")
public class TomcatConfiguration {

	@NotBlank
	private String path;
	@Valid
	private Db db;
	
	@Getter
	@Setter
	@Validated
	public static class Db {
		@NotBlank
		String url;
		@NotBlank
		String username;
		@NotNull
		String password;
	}
}
