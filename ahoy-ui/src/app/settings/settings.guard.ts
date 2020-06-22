import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {Observable, of} from 'rxjs';
import {flatMap} from 'rxjs/operators';
import {GitSettingsService} from './git-settings/git-settings.service';
import {ArgoSettingsService} from './argo-settings/argo-settings.service';

@Injectable({
  providedIn: 'root'
})
export class SettingsGuard implements CanActivate {
  private settingsConfigured = false;

  constructor(private gitSettingsService: GitSettingsService,
              private argoSettingsService: ArgoSettingsService,
              private router: Router) {
  }

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if (!this.settingsConfigured) {
      return this.gitSettingsService.exists()
        .pipe(
          flatMap((exists: boolean) => {
            if (!exists) {
              this.router.navigate(['/settings/git'], {queryParams: {setup: 'true'}});
            } else {
              return this.argoSettingsService.exists();
            }
            return of(exists);
          }),
          flatMap((exists: boolean) => {
            this.settingsConfigured = exists;
            if (!exists) {
              this.router.navigate(['/settings/argo'], {queryParams: {setup: 'true'}});
            }
            return of(exists);
          }));
    }

    return true;
  }
}
