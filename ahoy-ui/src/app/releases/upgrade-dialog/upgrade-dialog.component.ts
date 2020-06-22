import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {Release, ReleaseVersion} from '../release';

@Component({
  selector: 'app-upgrade-dialog',
  templateUrl: './upgrade-dialog.component.html',
  styleUrls: ['./upgrade-dialog.component.scss']
})
export class UpgradeDialogComponent implements OnInit {
  environmentRelease: EnvironmentRelease;
  release: Release;
  releaseVersion: ReleaseVersion;
  version: string;

  constructor(@Inject(MAT_DIALOG_DATA) data) {
    this.environmentRelease = data.environmentRelease;
    this.release = this.environmentRelease.release as Release;
    this.releaseVersion = data.releaseVersion;
  }

  ngOnInit() {
  }

}
