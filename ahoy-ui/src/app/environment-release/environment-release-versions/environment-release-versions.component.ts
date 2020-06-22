import {Component, Input, OnInit} from '@angular/core';
import {EnvironmentRelease} from '../environment-release';

@Component({
  selector: 'app-environment-release-versions',
  templateUrl: './environment-release-versions.component.html',
  styleUrls: ['./environment-release-versions.component.scss']
})
export class EnvironmentReleaseVersionsComponent implements OnInit {
  @Input() environmentRelease: EnvironmentRelease;

  constructor() { }

  ngOnInit() {
  }

}
