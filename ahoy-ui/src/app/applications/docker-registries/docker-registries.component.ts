import {Component, Input, OnInit} from '@angular/core';
import {DockerRegistry} from '../../settings/docker-settings/docker-settings';
import {ApplicationVersion} from '../application';
import {DockerSettingsService} from '../../settings/docker-settings/docker-settings.service';

@Component({
  selector: 'app-docker-registries',
  templateUrl: './docker-registries.component.html',
  styleUrls: ['./docker-registries.component.scss']
})
export class DockerRegistriesComponent implements OnInit {
  @Input() applicationVersion: ApplicationVersion;
  dockerRegistries: DockerRegistry[];

  constructor(private settingsService: DockerSettingsService) {
  }

  ngOnInit(): void {
    this.settingsService.get()
      .subscribe((settings) => {
        this.dockerRegistries = settings.dockerRegistries ? settings.dockerRegistries : [];
      });
  }

  compareRegistries(r1: DockerRegistry, r2: DockerRegistry): boolean {
    if (!r1) {
      return !r2;
    }

    if (!r2) {
      return !r1;
    }

    return r1.id === r2.id;
  }
}
