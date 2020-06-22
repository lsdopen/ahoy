package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Profile("dev")
@Slf4j
public class DevConfiguration {

	@Bean
	public DataSourceInitializer dataSourceInitializer(@Qualifier("dataSource") final DataSource dataSource) {
		try {
			Path sqlFile = Paths.get(System.getProperty("user.home"), ".ahoy-dev.sql");
			if (Files.exists(sqlFile) && isEmpty(dataSource)) {
				log.info("Initialising database from dev script: {}", sqlFile);
				ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
				resourceDatabasePopulator.addScript(new FileSystemResource(sqlFile));
				resourceDatabasePopulator.setCommentPrefix("###%%%"); // fake comment prefix so the parser does not parse certificate values as a comment line
				DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
				dataSourceInitializer.setDataSource(dataSource);
				dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
				return dataSourceInitializer;
			} else {
				log.debug("Skipping initialising database, no script or database not empty..");
				return null;
			}
		} catch (Exception e) {
			log.error("Failed to check if database needs to be initialised", e);
			return null;
		}
	}

	private boolean isEmpty(DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection();
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery("select * from Cluster")) {
			return !resultSet.next();
		}
	}
}
