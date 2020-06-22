package za.co.lsd.ahoy.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@Slf4j
public class AhoyServerConfiguration {

	@Bean
	public ExecutorService deploymentTaskExecutor() {
		return Executors.newSingleThreadExecutor(r -> new Thread(r, "deployment"));
	}
}
