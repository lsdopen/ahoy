package za.co.lsd.ahoy.server.environments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RepositoryRestController
@RequestMapping("/environments")
@Slf4j
public class EnvironmentsController {
	private final EnvironmentService environmentService;

	public EnvironmentsController(EnvironmentService environmentService) {
		this.environmentService = environmentService;
	}

	@PostMapping("/create")
	public ResponseEntity<?> create(@RequestBody Environment environment) {

		Environment newEnvironment = environmentService.create(environment);

		return new ResponseEntity<>(newEnvironment, new HttpHeaders(), HttpStatus.OK);
	}

	@DeleteMapping("/destroy/{environmentId}")
	public ResponseEntity<?> destroy(@PathVariable Long environmentId) {

		Environment environment = environmentService.destroy(environmentId);

		return new ResponseEntity<>(environment, new HttpHeaders(), HttpStatus.OK);
	}

	@PostMapping("/duplicate/{sourceEnvironmentId}/{destEnvironmentId}")
	public ResponseEntity<?> duplicate(@PathVariable Long sourceEnvironmentId, @PathVariable Long destEnvironmentId) {

		Environment destEnvironment = environmentService.duplicate(sourceEnvironmentId, destEnvironmentId);

		return new ResponseEntity<>(destEnvironment, new HttpHeaders(), HttpStatus.OK);
	}
}
