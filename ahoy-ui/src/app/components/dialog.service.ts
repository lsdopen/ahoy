/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
