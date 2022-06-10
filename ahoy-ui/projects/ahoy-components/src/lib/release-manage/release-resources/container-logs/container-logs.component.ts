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

import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ResourceNode} from '../../resource';
import {Subscription} from 'rxjs';
import {EnvironmentRelease} from '../../../environment-release/environment-release';
import {PodLog} from '../../log';
import {ReleaseManageService} from '../../release-manage.service';
import {LoggerService} from '../../../util/logger.service';

@Component({
  selector: 'app-container-logs',
  templateUrl: './container-logs.component.html',
  styleUrls: ['./container-logs.component.scss']
})
export class ContainerLogsComponent implements OnInit, OnDestroy {
  @Input() environmentRelease: EnvironmentRelease;
  @Input() podResourceNode: ResourceNode;
  @Input() container: any;
  logsSubscription: Subscription;
  logsContent = '';
  tailLogs = true;
  private tailLogsInterval: number;

  constructor(private releaseManageService: ReleaseManageService,
              private log: LoggerService) {
  }

  ngOnInit(): void {
    this.log.debug('Subscribing to logs: ', this.container.name);
    this.logsSubscription = this.releaseManageService.logs(this.environmentRelease.id, this.podResourceNode.name, this.podResourceNode.namespace, this.container.name)
      .subscribe((podLog: PodLog) => {
        if (podLog.result && podLog.result.content) {
          this.logsContent += podLog.result.content + '\n';
          this.scrollToEnd();

        } else if (podLog.error) {
          this.log.warn(`Error retrieving pod logs: ${podLog.error.message}, HTTP status: ${podLog.error.http_status}, code: ${podLog.error.http_code}`);
        }
      });

    this.tailLogsInterval = window.setInterval(() => {
      this.scrollToEnd();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.logsSubscription) {
      this.log.debug('Unsubscribing from logs: ', this.container.name);
      this.logsSubscription.unsubscribe();
      this.logsSubscription = null;
      this.logsContent = '';
    }
    clearInterval(this.tailLogsInterval);
  }

  private scrollToEnd() {
    if (this.tailLogs) {
      const logsTextArea = document.getElementById('logs' + this.container.name);
      if (logsTextArea) {
        logsTextArea.scrollTop = logsTextArea.scrollHeight;
      }
    }
  }
}
