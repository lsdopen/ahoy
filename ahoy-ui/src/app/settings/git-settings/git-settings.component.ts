import {Component, OnInit} from '@angular/core';
import {GitSettings} from './git-settings';
import {GitSettingsService} from './git-settings.service';
import {Notification} from '../../notifications/notification';
import {NotificationsService} from '../../notifications/notifications.service';

@Component({
  selector: 'app-git-settings',
  templateUrl: './git-settings.component.html',
  styleUrls: ['./git-settings.component.scss']
})
export class GitSettingsComponent implements OnInit {
  gitSettings: GitSettings;
  hideGitPassword = true;

  constructor(private gitSettingsService: GitSettingsService,
              private notificationsService: NotificationsService) {
  }

  ngOnInit(): void {
    this.gitSettingsService.get()
      .subscribe((gitSettings) => {
        this.gitSettings = gitSettings;
      });
  }

  save() {
    const notification = new Notification('Saved git settings');
    this.gitSettingsService.save(this.gitSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }
}
