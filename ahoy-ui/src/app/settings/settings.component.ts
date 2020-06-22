import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  setup: boolean;

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.children.map(r => {
      const setup = Boolean(r.snapshot.queryParamMap.get('setup'));
      if (setup) {
        this.setup = true;
      }
    });
  }
}
