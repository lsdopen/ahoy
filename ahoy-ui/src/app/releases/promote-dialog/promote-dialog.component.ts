import {Component, Inject, OnInit} from '@angular/core';
import {Environment} from '../../environments/environment';
import {EnvironmentService} from '../../environments/environment.service';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {Release, ReleaseVersion} from '../release';
import {Cluster} from '../../clusters/cluster';

@Component({
  selector: 'app-promote-dialog',
  templateUrl: './promote-dialog.component.html',
  styleUrls: ['./promote-dialog.component.scss']
})
export class PromoteDialogComponent implements OnInit {
  environmentRelease: EnvironmentRelease;
  environments: Environment[];
  selected: Environment;
  release: Release;
  releaseVersion: ReleaseVersion;
  cluster: Cluster;

  constructor(
    private environmentService: EnvironmentService,
    @Inject(MAT_DIALOG_DATA) data) {
    this.environmentRelease = data.environmentRelease;
    this.release = this.environmentRelease.release as Release;
    this.releaseVersion = data.releaseVersion;
    this.cluster = (this.environmentRelease.environment as Environment).cluster;
  }

  ngOnInit() {
    this.environmentService.getAllForPromotion(this.environmentRelease)
      .subscribe(environments => this.environments = environments);
  }
}
