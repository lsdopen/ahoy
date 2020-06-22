import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {Confirmation} from './confirm';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent implements OnInit {
  confirmation: Confirmation;

  constructor(@Inject(MAT_DIALOG_DATA) data) {
    this.confirmation = data;
  }

  ngOnInit() {
  }
}
