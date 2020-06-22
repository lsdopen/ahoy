import {Injectable} from '@angular/core';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {ConfirmDialogComponent} from './confirm-dialog/confirm-dialog.component';
import {Observable} from 'rxjs';
import {Description} from './description-dialog/description';
import {DescriptionDialogComponent} from './description-dialog/description-dialog.component';
import {Confirmation} from './confirm-dialog/confirm';

@Injectable({
  providedIn: 'root'
})
export class DialogService {

  constructor(private dialog: MatDialog) {
  }

  public showConfirmDialog(confirmation: Confirmation): Observable<Confirmation> {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = confirmation;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  public showDescriptionDialog(description: Description): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.data = description;

    this.dialog.open(DescriptionDialogComponent, dialogConfig);
  }
}
