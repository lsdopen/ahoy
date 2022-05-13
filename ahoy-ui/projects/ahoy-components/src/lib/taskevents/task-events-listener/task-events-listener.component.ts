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

import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {TaskEvent} from '../task-events';
import {TaskEventsService} from '../task-events.service';

@Component({
  selector: 'app-task-events-listener',
  templateUrl: './task-events-listener.component.html',
  styleUrls: ['./task-events-listener.component.scss']
})
export class TaskEventsListenerComponent implements OnInit, OnDestroy {
  @Output() taskEventEmitter = new EventEmitter<TaskEvent>();
  private taskSubscription;

  constructor(
    private taskEventsService: TaskEventsService) {
  }

  ngOnInit() {
    this.taskSubscription = this.taskEventsService.taskEvents
      .subscribe((taskEvent) => this.taskEventEmitter.emit(taskEvent));
  }

  ngOnDestroy(): void {
    this.taskSubscription.unsubscribe();
  }
}
