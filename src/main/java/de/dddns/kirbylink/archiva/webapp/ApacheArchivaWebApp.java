package de.dddns.kirbylink.archiva.webapp;

import java.io.File;

import javax.sql.DataSource;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import de.dddns.kirbylink.archiva.config.TomcatConfiguration;
import de.dddns.kirbylink.archiva.config.TomcatConfiguration.Db;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApacheArchivaWebApp {
	
	private final TomcatConfiguration tomcatConfiguration;

	@Bean
	public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
		return new TomcatServletWebServerFactory() {
			@Override
			protected TomcatWebServer getTomcatWebServer(org.apache.catalina.startup.Tomcat tomcat) {
				log.info("Configure TomcatWebServer");
				new File(tomcat.getServer().getCatalinaBase(), "webapps").mkdirs();
				Context context = tomcat.addWebapp("", tomcatConfiguration.getPath());
				context.setParentClassLoader(getClass().getClassLoader());
				
				Db db = tomcatConfiguration.getDb();
				
				ContextResource contextResource = new ContextResource();
				contextResource.setName("jdbc/users");
				contextResource.setType(DataSource.class.getName());
				contextResource.setAuth("Container");
				contextResource.setProperty("username", db.getUsername());
				contextResource.setProperty("password", db.getPassword());
				contextResource.setProperty("driverClassName", "org.apache.derby.jdbc.EmbeddedDriver");
				contextResource.setProperty("url", db.getUrl());
				context.getNamingResources().addResource(contextResource);
				
				tomcat.enableNaming();
				
				((StandardJarScanner) context.getJarScanner()).setScanManifest(false);

				return super.getTomcatWebServer(tomcat);
			}
		};
	}
}
