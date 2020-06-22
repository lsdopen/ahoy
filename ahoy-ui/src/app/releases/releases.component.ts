import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EnvironmentService} from '../environments/environment.service';
import {Environment} from '../environments/environment';
import {LoggerService} from '../util/logger.service';
import {EnvironmentRelease, EnvironmentReleaseId} from '../environment-release/environment-release';
import {EnvironmentReleaseService} from '../environment-release/environment-release.service';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {filter, flatMap} from 'rxjs/operators';
import {AddReleaseDialogComponent} from './add-release-dialog/add-release-dialog.component';
import {ReleasesService} from './releases.service';
import {Release} from './release';
import {TaskEvent} from '../taskevents/task-events';
import {ReleaseService} from '../release/release.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-releases',
  templateUrl: './releases.component.html',
  styleUrls: ['./releases.component.scss']
})
export class ReleasesComponent implements OnInit {
  environments: Environment[] = undefined;
  environmentReleases: EnvironmentRelease[] = undefined;
  selectedEnvironment: Environment;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private environmentService: EnvironmentService,
              private environmentReleaseService: EnvironmentReleaseService,
              private releasesService: ReleasesService,
              private releaseService: ReleaseService,
              private log: LoggerService,
              private dialog: MatDialog,
              private dialogService: DialogService) {
  }

  ngOnInit() {
    const environmentId = +this.route.snapshot.queryParamMap.get('environmentId');

    this.environmentService.getAll().subscribe((environments) => {
      this.environments = environments;

      if (environmentId === 0) {
        this.environmentService.getLastUsedId().subscribe((lastUsedEnvironmentId) => {
          if (lastUsedEnvironmentId !== 0) {
            this.getReleases(lastUsedEnvironmentId);
          }
        });
      } else {
        this.getReleases(environmentId);
      }
    });
  }

  private getReleases(environmentId) {
    this.log.debug('getting environment releases for environmentId=', environmentId);
    this.environmentService.get(environmentId)
      .subscribe(env => {
        this.selectedEnvironment = env;
        this.environmentReleaseService.getReleasesByEnvironment(environmentId)
          .subscribe(envReleases => this.environmentReleases = envReleases);
      });
  }

  addRelease() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = this.selectedEnvironment;

    const dialogRef = this.dialog.open(AddReleaseDialogComponent, dialogConfig);
    dialogRef.afterClosed().pipe(
      filter((result) => result !== undefined), // cancelled
      flatMap((release: Release) => {
        const environmentRelease = new EnvironmentRelease();
        environmentRelease.id = new EnvironmentReleaseId();

        environmentRelease.environment = this.environmentService.link(this.selectedEnvironment.id);
        environmentRelease.release = this.releasesService.link(release.id);

        return this.environmentReleaseService.save(environmentRelease);
      })
    ).subscribe(() => {
      this.getReleases(this.selectedEnvironment.id);
    });
  }

  removeRelease(environmentRelease: EnvironmentRelease) {
    const confirmation = new Confirmation(`Are you sure you want to remove ${(environmentRelease.release as Release).name} from ${(environmentRelease.environment as Environment).name}?`);
    confirmation.verify = true;
    confirmation.verifyText = (environmentRelease.release as Release).name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.releaseService.remove(environmentRelease)
        .subscribe(() => this.getReleases(this.selectedEnvironment.id));
    });
  }

  compareEnvironments(e1: Environment, e2: Environment): boolean {
    if (e1 === null) {
      return e2 === null;
    }

    if (e2 === null) {
      return e1 === null;
    }

    return e1.id === e2.id;
  }

  environmentChanged() {
    this.getReleases(this.selectedEnvironment.id);
  }

  taskEventOccurred(event: TaskEvent) {
    if (event.releaseStatusChangedEvent) {
      const statusChangedEvent = event.releaseStatusChangedEvent;
      if (this.selectedEnvironment.id === statusChangedEvent.environmentReleaseId.environmentId) {
        setTimeout(() => {
          this.getReleases(this.selectedEnvironment.id);
        }, 1000);
      }
    }
  }
}
