import {Component, OnInit} from '@angular/core';
import {ApplicationService} from './application.service';
import {Application} from './application';
import {ActivatedRoute} from '@angular/router';
import {LoggerService} from '../util/logger.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {filter} from 'rxjs/operators';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss']
})
export class ApplicationsComponent implements OnInit {
  applications: Application[] = undefined;

  constructor(
    private route: ActivatedRoute,
    private applicationService: ApplicationService,
    private log: LoggerService,
    private dialogService: DialogService) {
  }

  ngOnInit() {
    this.getApplications();
  }

  private getApplications() {
    this.log.debug('Getting all applications');
    this.applicationService.getAll()
      .subscribe(applications => this.applications = applications);
  }

  delete(application: Application) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${application.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = application.name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.applicationService.delete(application)
        .subscribe(() => this.getApplications());
    });
  }
}
