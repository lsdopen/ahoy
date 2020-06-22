import {Component, OnInit} from '@angular/core';
import {DockerRegistry, DockerSettings} from './docker-settings';
import {DockerSettingsService} from './docker-settings.service';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';

@Component({
  selector: 'app-docker-settings',
  templateUrl: './docker-settings.component.html',
  styleUrls: ['./docker-settings.component.scss']
})
export class DockerSettingsComponent implements OnInit {
  dockerSettings: DockerSettings;
  hideDockerPassword = true;
  selectedIndex: number;

  constructor(private dockerSettingsService: DockerSettingsService,
              private notificationsService: NotificationsService) {
  }

  ngOnInit(): void {
    this.dockerSettingsService.get()
      .subscribe((dockerSettings) => {
        this.dockerSettings = dockerSettings;
        if (!this.dockerSettings.dockerRegistries) {
          this.dockerSettings.dockerRegistries = [];
        }
      });
  }

  save() {
    const notification = new Notification('Saved docker settings');
    this.dockerSettingsService.save(this.dockerSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }

  addDockerRegistry() {
    this.dockerSettings.dockerRegistries.push(new DockerRegistry());
    this.selectedIndex = this.dockerSettings.dockerRegistries.length - 1;
  }

  deleteRegistry() {
    this.dockerSettings.dockerRegistries.splice(this.selectedIndex, 1);
    this.selectedIndex = this.selectedIndex - 1;
  }
}
