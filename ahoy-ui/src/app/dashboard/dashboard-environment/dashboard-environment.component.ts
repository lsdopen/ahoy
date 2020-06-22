import {Component, Input, OnInit} from '@angular/core';
import {Environment} from '../../environments/environment';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {TaskEvent} from '../../taskevents/task-events';

@Component({
  selector: 'app-dashboard-environment',
  templateUrl: './dashboard-environment.component.html',
  styleUrls: ['./dashboard-environment.component.scss']
})
export class DashboardEnvironmentComponent implements OnInit {
  @Input() environment: Environment;
  environmentReleases: EnvironmentRelease[];

  constructor(private environmentReleaseService: EnvironmentReleaseService) {
  }

  ngOnInit() {
    this.getReleases();
  }

  private getReleases() {
    this.environmentReleaseService.getReleasesByEnvironment(this.environment.id)
      .subscribe(environmentReleases => this.environmentReleases = environmentReleases);
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.environment.id === statusChangedEvent.environmentReleaseId.environmentId) {
        setTimeout(() => {
          this.getReleases();
        }, 1000);
      }
    }
  }
}
