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

import {HttpClientTestingModule} from '@angular/common/http/testing';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {OAuthService} from 'angular-oauth2-oidc';
import {of} from 'rxjs';
import {ParamMapStub} from '../../../testing/param-map-stub';
import {TestOAuthService} from '../../../testing/test-o-auth-service';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {Cluster} from '../../clusters/cluster';
import {ClusterService} from '../../clusters/cluster.service';
import {ReleaseManageService} from '../../release-manage/release-manage.service';
import {Environment} from '../environment';
import {EnvironmentService} from '../environment.service';
import {EnvironmentDetailComponent} from './environment-detail.component';
import {EnvironmentRelease} from '../../environment-release/environment-release';

describe('EnvironmentDetailComponent', () => {
  let component: EnvironmentDetailComponent;
  let fixture: ComponentFixture<EnvironmentDetailComponent>;

  let environmentService: EnvironmentService;
  let clusterService: ClusterService;
  let releaseManageService: ReleaseManageService;

  const paramMap = new ParamMapStub({});
  const queryParamMap = new ParamMapStub({});

  let environmentGetSpy;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EnvironmentDetailComponent],
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      providers: [
        {provide: ComponentFixtureAutoDetect, useValue: true},
        {provide: AppBreadcrumbService, useClass: AppBreadcrumbService},
        {provide: OAuthService, useClass: TestOAuthService},
        {provide: ActivatedRoute, useValue: {snapshot: {paramMap, queryParamMap}}}
      ]
    }).compileComponents();

    environmentService = TestBed.inject(EnvironmentService);
    clusterService = TestBed.inject(ClusterService);
    releaseManageService = TestBed.inject(ReleaseManageService);

    const environments: Environment[] = [];
    const environment = new Environment();
    environment.id = 1;
    environment.name = 'env-1';
    environments.push(environment);

    environmentGetSpy = spyOn(environmentService, 'get').and.returnValue(of(environment));
    spyOn(environmentService, 'getAll').and.returnValue(of(environments));

    const clusters: Cluster[] = [];
    const cluster = new Cluster();
    cluster.id = 1;
    cluster.name = 'cluster-1';
    clusters.push(cluster);

    spyOn(clusterService, 'getAll').and.returnValue(of(clusters));
    spyOn(clusterService, 'link').and.callThrough();
  });

  describe('test new', () => {
    let environmentDuplicateSpy;
    let releaseManagePromoteSpy;

    beforeEach(() => {
      // init
    });

    describe('test new simple', () => {

      beforeEach(() => {
        paramMap.setParams({id: 'new'});
        queryParamMap.setParams({});

        environmentDuplicateSpy = spyOn(environmentService, 'duplicate').and.callFake((sourceEnv, destEnv) => of(destEnv));
        releaseManagePromoteSpy = spyOn(releaseManageService, 'promote');

        fixture = TestBed.createComponent(EnvironmentDetailComponent);
        component = fixture.componentInstance;
      });

      it('should save new', () => {
        // init
        expect(component).toBeTruthy();
        expect(component.editMode).toBe(false);
        expect(component.clusters).toHaveSize(1);
        expect(component.environmentsForValidation).toHaveSize(1);
        expect(component.environment).toBeTruthy();
        expect(component.sourceEnvironment).toBeFalsy();
        expect(component.promoteEnvironmentReleaseId).toBeFalsy();

        // given
        component.environment.name = 'new-environment';
        component.cluster = component.clusters[0];
        const savedEnvironment = new Environment();
        savedEnvironment.id = 2;
        savedEnvironment.name = 'new-environment';
        const save = spyOn(environmentService, 'save').and.returnValue(of(savedEnvironment));

        // when
        component.save();

        // then
        expect(save).toHaveBeenCalledTimes(1);
        expect(environmentDuplicateSpy).toHaveBeenCalledTimes(0);
        expect(releaseManagePromoteSpy).toHaveBeenCalledTimes(0);
      });
    });

    describe('test new duplicate', () => {

      beforeEach(() => {
        paramMap.setParams({id: 'new'});
        queryParamMap.setParams({sourceEnvironmentId: '1'});

        environmentDuplicateSpy = spyOn(environmentService, 'duplicate').and.callFake((sourceEnv, destEnv) => of(destEnv));
        releaseManagePromoteSpy = spyOn(releaseManageService, 'promote');

        fixture = TestBed.createComponent(EnvironmentDetailComponent);
        component = fixture.componentInstance;
      });

      it('should save duplicated', () => {
        // init
        expect(component).toBeTruthy();
        expect(component.editMode).toBe(false);
        expect(component.clusters).toHaveSize(1);
        expect(component.environmentsForValidation).toHaveSize(1);
        expect(component.environment).toBeTruthy();
        expect(component.sourceEnvironment).toBeTruthy();
        expect(component.promoteEnvironmentReleaseId).toBeFalsy();

        // given
        component.environment.name = 'new-duplicated-environment';
        component.cluster = component.clusters[0];
        const savedEnvironment = new Environment();
        savedEnvironment.id = 2;
        savedEnvironment.name = 'new-duplicated-environment';
        const save = spyOn(environmentService, 'save').and.returnValue(of(savedEnvironment));

        // when
        component.save();

        // then
        expect(save).toHaveBeenCalledTimes(1);
        expect(environmentDuplicateSpy).toHaveBeenCalledTimes(1);
        expect(releaseManagePromoteSpy).toHaveBeenCalledTimes(0);
      });
    });

    describe('test new promote', () => {

      beforeEach(() => {
        paramMap.setParams({id: 'new'});
        queryParamMap.setParams({environmentId: '1', releaseId: '1', copyEnvironmentConfig: 'true'});

        environmentDuplicateSpy = spyOn(environmentService, 'duplicate').and.callFake((sourceEnv, destEnv) => of(destEnv));
        releaseManagePromoteSpy = spyOn(releaseManageService, 'promote').and.callFake((envRelId, promoteOptions) => of(new EnvironmentRelease()));

        fixture = TestBed.createComponent(EnvironmentDetailComponent);
        component = fixture.componentInstance;
      });

      it('should save promoted', () => {
        // init
        expect(component).toBeTruthy();
        expect(component.editMode).toBe(false);
        expect(component.clusters).toHaveSize(1);
        expect(component.environmentsForValidation).toHaveSize(1);
        expect(component.environment).toBeTruthy();
        expect(component.sourceEnvironment).toBeFalsy();
        expect(component.promoteEnvironmentReleaseId).toBeTruthy();
        expect(component.copyEnvironmentConfig).toBeTrue();

        // given
        component.environment.name = 'new-promoted-environment';
        component.cluster = component.clusters[0];
        const savedEnvironment = new Environment();
        savedEnvironment.id = 2;
        savedEnvironment.name = 'new-promoted-environment';
        const save = spyOn(environmentService, 'save').and.returnValue(of(savedEnvironment));

        // when
        component.save();

        // then
        expect(save).toHaveBeenCalledTimes(1);
        expect(environmentDuplicateSpy).toHaveBeenCalledTimes(0);
        expect(releaseManagePromoteSpy).toHaveBeenCalledTimes(1);
      });
    });

  });

  describe('test edit', () => {
    beforeEach(() => {
      paramMap.setParams({id: '1'});
      queryParamMap.setParams({});

      fixture = TestBed.createComponent(EnvironmentDetailComponent);
      component = fixture.componentInstance;
    });

    it('should init', () => {
      // init
      expect(component).toBeTruthy();
      expect(component.editMode).toBe(true);
      expect(component.clusters).toHaveSize(1);
      expect(component.environmentsForValidation).toHaveSize(1);
      expect(component.environment).toBeTruthy();
      expect(component.sourceEnvironment).toBeFalsy();
      expect(component.promoteEnvironmentReleaseId).toBeFalsy();
      expect(component.copyEnvironmentConfig).toBeFalsy();

      // given
      const savedEnvironment = new Environment();
      savedEnvironment.id = 1;
      savedEnvironment.name = 'edited-environment';
      const save = spyOn(environmentService, 'save').and.returnValue(of(savedEnvironment));

      // when
      component.save();

      // then
      expect(save).toHaveBeenCalledTimes(1);
    });
  });
});
