import {Component, Inject, OnInit} from '@angular/core';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {Release, ReleaseVersion} from '../release';

@Component({
  selector: 'app-copy-environment-config-dialog',
  templateUrl: './copy-environment-config-dialog.component.html',
  styleUrls: ['./copy-environment-config-dialog.component.scss']
})
export class CopyEnvironmentConfigDialogComponent implements OnInit {
  private environmentRelease: EnvironmentRelease;
  destReleaseVersion: ReleaseVersion;
  releaseVersions: ReleaseVersion[];
  selectedReleaseVersion: ReleaseVersion;

  constructor(@Inject(MAT_DIALOG_DATA) data) {
    this.environmentRelease = data[0];
    this.destReleaseVersion = data[1];
  }

  ngOnInit() {
    this.releaseVersions = (this.environmentRelease.release as Release).releaseVersions
      .filter(rel => rel.id !== this.destReleaseVersion.id);
  }
}
