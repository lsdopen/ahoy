import {Component, Inject, OnInit} from '@angular/core';
import {Environment} from '../../environments/environment';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {Release} from '../release';
import {ReleasesService} from '../releases.service';

@Component({
  selector: 'app-add-release-dialog',
  templateUrl: './add-release-dialog.component.html',
  styleUrls: ['./add-release-dialog.component.scss']
})
export class AddReleaseDialogComponent implements OnInit {
  releases: Release[];
  selected: Release;
  environment: Environment;

  constructor(
    private releasesService: ReleasesService,
    @Inject(MAT_DIALOG_DATA) data) {
    this.environment = data;
  }

  ngOnInit() {
    this.releasesService.getAllForAdd(this.environment.id)
      .subscribe(rels => this.releases = rels);
  }
}
