/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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
import {DialogService, DynamicDialogConfig} from 'primeng/dynamicdialog';
import {Observable} from 'rxjs';
import {Confirmation} from './confirm-dialog/confirm';
import {ConfirmDialogComponent} from './confirm-dialog/confirm-dialog.component';
import {Description} from './description-dialog/description';
import {DescriptionDialogComponent} from './description-dialog/description-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class DialogUtilService {

  constructor(private dialogService: DialogService) {
  }

  public showConfirmDialog(confirmation: Confirmation): Observable<Confirmation> {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = confirmation.title;
    dialogConfig.data = confirmation;

    const dialogRef = this.dialogService.open(ConfirmDialogComponent, dialogConfig);
    return dialogRef.onClose;
  }

  public showDescriptionDialog(description: Description): void {
    const dialogConfig = new DynamicDialogConfig();
    dialogConfig.header = description.title;
    dialogConfig.data = description;

    this.dialogService.open(DescriptionDialogComponent, dialogConfig);
  }
}
