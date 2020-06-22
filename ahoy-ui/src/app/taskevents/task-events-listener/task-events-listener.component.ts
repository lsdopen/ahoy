import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {TaskEventsService} from '../task-events.service';
import {TaskEvent} from '../task-events';

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
