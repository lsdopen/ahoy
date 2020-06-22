import {Component, OnInit} from '@angular/core';
import {ArgoSettings} from './argo-settings';
import {ArgoSettingsService} from './argo-settings.service';
import {NotificationsService} from '../../notifications/notifications.service';
import {Notification} from '../../notifications/notification';

@Component({
  selector: 'app-argo-settings',
  templateUrl: './argo-settings.component.html',
  styleUrls: ['./argo-settings.component.scss']
})
export class ArgoSettingsComponent implements OnInit {
  argoSettings: ArgoSettings;
  hideArgoToken = true;

  constructor(private argoSettingsService: ArgoSettingsService,
              private notificationsService: NotificationsService) { }

  ngOnInit(): void {
    this.argoSettingsService.get()
      .subscribe((argoSettings) => {
        this.argoSettings = argoSettings;
      });
  }

  save() {
    const notification = new Notification('Saved argocd settings');
    this.argoSettingsService.save(this.argoSettings)
      .subscribe(() => this.notificationsService.notification(notification));
  }
}
