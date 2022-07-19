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
import {EMPTY, Observable, Subject} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {Notification} from '../notifications/notification';
import {NotificationsService} from '../notifications/notifications.service';
import {LoggerService} from '../util/logger.service';
import {RestClientService} from '../util/rest-client.service';
import {ArgoSettings} from './argo-settings/argo-settings';
import {DockerSettings} from './docker-settings/docker-settings';
import {GitSettings} from './git-settings/git-settings';

@Injectable({
  providedIn: 'root'
})
export class SettingsService {
  private defaultKnownHosts =
    'bitbucket.org ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAubiN81eDcafrgMeLzaFPsw2kNvEcqTKl/VqLat/MaB33pZy0y3rJZtnqwR2qOOvbwKZYKiEO1O6VqNEBxKvJJelCq0dTXWT5pbO2gDXC6h6QDXCaHo6pOHGPUy+YBaGQRGuSusMEASYiWunYN0vCAI8QaXnWMXNMdFP3jHAJH0eDsoiGnLPBlBp4TNm6rYI74nMzgz3B9IikW4WVK+dc8KZJZWYjAuORU3jc1c/NPskD2ASinf8v3xnfXeukU0sJ5N6m5E8VLjObPEO+mN2t/FZTMZLiFqPWc/ALSqnMnnhwrNi2rbfg/rd/IpL8Le3pSBne8+seeFVBoGqzHM9yXw==\n' +
    'github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=\n' +
    'gitlab.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBFSMqzJeV9rUzU4kWitGjeR4PWSa29SPqJ1fVkhtj3Hw9xjLVXVYrU9QlYWrOLXBpQ6KWjbjTDTdDkoohFzgbEY=\n' +
    'gitlab.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAfuCHKVTjquxvt6CM6tdG4SLp1Btn/nOeHHE5UOzRdf\n' +
    'gitlab.com ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCsj2bNKTBSpIYDEGk9KxsGh3mySTRgMtXL583qmBpzeQ+jqCMRgBqB98u3z++J1sKlXHWfM9dyhSevkMwSbhoR8XIq/U0tCNyokEi/ueaBMCvbcTHhO7FcwzY92WK4Yt0aGROY5qX2UKSeOvuP4D6TPqKF1onrSzH9bx9XUf2lEdWT/ia1NEKjunUqu1xOB/StKDHMoX4/OKyIzuS0q/T1zOATthvasJFoPrAjkohTyaDUz2LN5JoH839hViyEG82yB+MjcFV5MU3N1l1QL3cVUCh93xSaua1N85qivl+siMkPGbO5xR/En4iEY6K2XPASUEMaieWVNTRCtJ4S8H+9\n' +
    'ssh.dev.azure.com ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC7Hr1oTWqNqOlzGJOfGJ4NakVyIzf1rXYd4d7wo6jBlkLvCA4odBlL0mDUyZ0/QUfTTqeu+tm22gOsv+VrVTMk6vwRU75gY/y9ut5Mb3bR5BV58dKXyq9A9UeB5Cakehn5Zgm6x1mKoVyf+FFn26iYqXJRgzIZZcZ5V6hrE0Qg39kZm4az48o0AUbf6Sp4SLdvnuMa2sVNwHBboS7EJkm57XQPVU3/QpyNLHbWDdzwtrlS+ez30S3AdYhLKEOxAG8weOnyrtLJAUen9mTkol8oII1edf7mWWbWVf0nBmly21+nZcmCTISQBtdcyPaEno7fFQMDD26/s0lfKob4Kw8H\n' +
    'vs-ssh.visualstudio.com ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC7Hr1oTWqNqOlzGJOfGJ4NakVyIzf1rXYd4d7wo6jBlkLvCA4odBlL0mDUyZ0/QUfTTqeu+tm22gOsv+VrVTMk6vwRU75gY/y9ut5Mb3bR5BV58dKXyq9A9UeB5Cakehn5Zgm6x1mKoVyf+FFn26iYqXJRgzIZZcZ5V6hrE0Qg39kZm4az48o0AUbf6Sp4SLdvnuMa2sVNwHBboS7EJkm57XQPVU3/QpyNLHbWDdzwtrlS+ez30S3AdYhLKEOxAG8weOnyrtLJAUen9mTkol8oII1edf7mWWbWVf0nBmly21+nZcmCTISQBtdcyPaEno7fFQMDD26/s0lfKob4Kw8H\n';
  private settingsChangedSubject = new Subject<any>();

  constructor(
    private log: LoggerService,
    private restClient: RestClientService,
    private notificationsService: NotificationsService) {
  }

  getGitSettings(): Observable<GitSettings> {
    const url = `/api/settings/git`;
    return this.restClient.get<GitSettings>(url, false, () => {
      const gitSettings = new GitSettings();
      gitSettings.sshKnownHosts = this.defaultKnownHosts;
      return gitSettings;
    }).pipe(
      tap((settings) => {
        this.log.debug('fetched git settings', settings);
      })
    );
  }

  gitSettingsExists(): Observable<boolean> {
    const url = `/api/settings/git/exists`;
    return this.restClient.exists(url, false).pipe(
      tap((exists) => {
        this.log.debug('checked git settings exists: ', exists);
      })
    );
  }

  saveGitSettings(gitSettings: GitSettings): Observable<void> {
    this.log.debug('saving git settings: ', gitSettings);

    return this.restClient.post<void>('/api/settings/git', gitSettings, true).pipe(
      tap(() => {
        this.log.debug('saved git settings');
        this.settingsChangedSubject.next(gitSettings);
      })
    );
  }

  testGitConnection(gitSettings: GitSettings): Observable<GitSettings> {
    const url = `/api/settings/git/test`;
    return this.restClient.post<null>(url, gitSettings, true).pipe(
      tap(() => {
        this.log.debug('tested connection to git repo', gitSettings);
        const text = `Successfully connected to git repo '${gitSettings.remoteRepoUri}'`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to connect to git repo ${gitSettings.remoteRepoUri}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  getArgoSettings(): Observable<ArgoSettings> {
    const url = `/api/settings/argo`;
    return this.restClient.get<ArgoSettings>(url, false, () => {
      return new ArgoSettings();
    }).pipe(
      tap((settings) => {
        this.log.debug('fetched argo settings', settings);
      })
    );
  }

  argoSettingsExists(): Observable<boolean> {
    const url = `/api/settings/argo/exists`;
    return this.restClient.exists(url, false).pipe(
      tap((exists) => {
        this.log.debug('checked argo settings exists: ', exists);
      })
    );
  }

  saveArgoSettings(argoSettings: ArgoSettings): Observable<void> {
    this.log.debug('saving argo settings: ', argoSettings);

    return this.restClient.post<void>('/api/settings/argo', argoSettings, true).pipe(
      tap(() => {
        this.log.debug('saved argo settings');
        this.settingsChangedSubject.next(argoSettings);
      })
    );
  }

  testArgoConnection(argoSettings: ArgoSettings): Observable<ArgoSettings> {
    const url = `/api/settings/argo/test`;
    return this.restClient.post<null>(url, argoSettings, true).pipe(
      tap(() => {
        this.log.debug('tested connection to argocd', argoSettings);
        const text = `Successfully connected to argocd '${argoSettings.argoServer}'`;
        this.notificationsService.notification(new Notification(text));
      }),
      catchError((error) => {
        const text = `Failed to connect to argocd ${argoSettings.argoServer}`;
        this.notificationsService.notification(new Notification(text, error));
        return EMPTY;
      })
    );
  }

  updateArgoKnownHosts(): Observable<void> {
    this.log.debug('updating argo knownhosts..');

    return this.restClient.post<void>('/api/settings/argo/updateKnownHosts').pipe(
      tap(() => {
        this.log.debug('updated argo knownhosts');
      })
    );
  }

  getDockerSettings(): Observable<DockerSettings> {
    const url = `/api/settings/docker`;
    return this.restClient.get<DockerSettings>(url, false, () => {
      return new DockerSettings();
    }).pipe(
      tap((settings) => {
        this.log.debug('fetched docker settings', settings);
      })
    );
  }

  dockerSettingsExists(): Observable<boolean> {
    const url = `/api/settings/docker`;
    return this.restClient.exists(url, false).pipe(
      tap((exists) => {
        this.log.debug('checked docker settings exists: ', exists);
      })
    );
  }

  saveDockerSettings(dockerSettings: DockerSettings): Observable<void> {
    this.log.debug('saving docker settings: ', dockerSettings);

    return this.restClient.post<void>('/api/settings/docker', dockerSettings, true).pipe(
      tap(() => {
        this.log.debug('saved docker settings');
        this.settingsChangedSubject.next(dockerSettings);
      })
    );
  }

  public settingsChanged(): Observable<any> {
    return this.settingsChangedSubject.asObservable();
  }
}
