import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {Description} from './description';

@Component({
  selector: 'app-description-dialog',
  templateUrl: './description-dialog.component.html',
  styleUrls: ['./description-dialog.component.scss']
})
export class DescriptionDialogComponent implements OnInit {
  description: Description;

  constructor(@Inject(MAT_DIALOG_DATA) data) {
    this.description = data;
  }

  ngOnInit() {
  }

}
