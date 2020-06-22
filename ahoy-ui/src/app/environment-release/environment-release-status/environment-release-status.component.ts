import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentRelease} from '../environment-release';

@Component({
  selector: 'app-environment-release-status',
  templateUrl: './environment-release-status.component.html',
  styleUrls: ['./environment-release-status.component.scss']
})
export class EnvironmentReleaseStatusComponent implements OnInit {
  @Input() environmentRelease: EnvironmentRelease;
  private applicationsReady: number;
  private applicationsTotal: number;

  constructor() {
  }

  ngOnInit() {
    this.applicationsReady = this.environmentRelease.applicationsReady ?
      this.environmentRelease.applicationsReady : 0;
    this.applicationsTotal = this.environmentRelease.currentReleaseVersion ?
      this.environmentRelease.currentReleaseVersion.applicationVersions.length : 0;
  }

  style(): string {
    if (this.applicationsReady === 0 && this.applicationsTotal > 0) {
      return 'status-error';

    } else if (this.applicationsReady < this.applicationsTotal) {
      return 'status-warn';

    } else {
      return 'status-success';
    }
  }
}
