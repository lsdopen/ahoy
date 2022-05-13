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

import {Directive, Input, OnInit, TemplateRef, ViewContainerRef} from '@angular/core';
import {Role} from './auth';
import {AuthService} from './auth.service';

@Directive({
  selector: '[appUserRole]'
})
export class UserRoleDirective implements OnInit {
  userRoles: Role[];

  constructor(private templateRef: TemplateRef<any>,
              private viewContainer: ViewContainerRef,
              private authService: AuthService) {
  }

  @Input()
  set appUserRole(roles: Role[]) {
    if (!roles || !roles.length) {
      throw new Error('Roles value is empty or missing');
    }

    this.userRoles = roles;
  }

  ngOnInit(): void {
    let hasAccess = false;
    if (this.authService.isAuthenticated() && this.userRoles) {
      hasAccess = this.authService.hasOneOfRole(this.userRoles);
    }
    if (hasAccess) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear();
    }
  }
}
