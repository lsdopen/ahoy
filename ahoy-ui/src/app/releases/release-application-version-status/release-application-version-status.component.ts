import {Component, Input, OnInit} from '@angular/core';
import {ApplicationVersion} from '../../applications/application';

@Component({
  selector: 'app-release-application-version-status',
  templateUrl: './release-application-version-status.component.html',
  styleUrls: ['./release-application-version-status.component.scss']
})
export class ReleaseApplicationVersionStatusComponent implements OnInit {
  @Input() applicationVersion: ApplicationVersion;

  constructor() { }

  ngOnInit() {
  }

}
