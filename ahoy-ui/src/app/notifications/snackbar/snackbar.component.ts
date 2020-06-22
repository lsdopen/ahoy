import {Component, Inject} from '@angular/core';
import {Notification} from '../notification';
import {MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';

@Component({
  selector: 'app-snackbar',
  templateUrl: './snackbar.component.html',
  styleUrls: ['./snackbar.component.scss']
})
export class SnackbarComponent {
  notification: Notification;

  constructor(@Inject(MAT_SNACK_BAR_DATA) public data: any) {
    this.notification = data.notification;
  }
}
