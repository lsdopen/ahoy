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

import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TreeNode} from 'primeng/api';
import {of} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {AppBreadcrumbService} from '../../app.breadcrumb.service';
import {EnvironmentRelease} from '../../environment-release/environment-release';
import {EnvironmentReleaseService} from '../../environment-release/environment-release.service';
import {Environment} from '../../environments/environment';
import {Release, ReleaseVersion} from '../../releases/release';
import {LoggerService} from '../../util/logger.service';
import {ReleaseManageService} from '../release-manage.service';
import {Event, ResourceNode} from '../resource';

@Component({
  selector: 'app-release-resources',
  templateUrl: './release-resources.component.html',
  styleUrls: ['./release-resources.component.scss']
})
export class ReleaseResourcesComponent implements OnInit {
  resources: TreeNode<ResourceNode>[];
  selectedNode: TreeNode<ResourceNode>;
  environmentRelease: EnvironmentRelease;
  releaseVersion: ReleaseVersion;
  events: Event[];
  selectedResourceNode: ResourceNode;
  tabActiveIndex: 0;
  podManifest: any;
  containers: any;
  containerTabActiveIndex: any;

  constructor(private route: ActivatedRoute,
              private location: Location,
              private releaseManageService: ReleaseManageService,
              private environmentReleaseService: EnvironmentReleaseService,
              private breadcrumbService: AppBreadcrumbService,
              private log: LoggerService) {
  }

  ngOnInit(): void {
    const environmentId = +this.route.snapshot.paramMap.get('environmentId');
    const releaseId = +this.route.snapshot.paramMap.get('releaseId');
    this.getResources(environmentId, releaseId);
  }

  private getResources(environmentId: number, releaseId: number) {
    this.environmentReleaseService.get(environmentId, releaseId).pipe(
      mergeMap((environmentRelease) => {
        this.environmentRelease = environmentRelease;
        this.releaseVersion = environmentRelease.currentReleaseVersion;

        this.setBreadcrumb();
        return of(environmentRelease);
      }),
      mergeMap((environmentRelease: EnvironmentRelease) => this.releaseManageService.resources(environmentRelease.id))
    ).subscribe((resourceNode) => {
      const rootTreeNode: TreeNode<ResourceNode> = {
        label: `${(this.environmentRelease.release as Release).name} - ${this.environmentRelease.currentReleaseVersion.version}`,
        expanded: true,
        children: [],
        data: resourceNode
      };
      this.resources = [rootTreeNode];
      this.loadResources(resourceNode, rootTreeNode);
    });
  }

  private loadResources(resourceNode: ResourceNode, treeNode: TreeNode): void {
    for (const childResourceNode of resourceNode.children) {
      const childNode: TreeNode<ResourceNode> = {
        label: `${childResourceNode.kind} - ${childResourceNode.name}`,
        expanded: !childResourceNode.leaf,
        icon: this.iconForKind(childResourceNode.kind),
        children: [],
        data: childResourceNode
      };
      treeNode.children.push(childNode);
      this.loadResources(childResourceNode, childNode);
    }
  }

  private setBreadcrumb() {
    const env = (this.environmentRelease.environment as Environment);
    const rel = (this.environmentRelease.release as Release);
    this.breadcrumbService.setItems([
      {label: env.key, routerLink: '/environments'},
      {label: rel.name, routerLink: `/release/${env.id}/${rel.id}/version/${this.releaseVersion.id}`},
      {label: this.releaseVersion.version, routerLink: `/release/${env.id}/${rel.id}/version/${this.releaseVersion.id}`},
      {label: 'resources'}
    ]);
  }

  nodeSelected(event: any) {
    this.selectedResourceNode = event.node.data as ResourceNode;
    this.tabActiveIndex = 0;

    if (this.selectedResourceNode.root) {
      this.events = [];

    } else {
      this.releaseManageService.events(this.environmentRelease.id,
        this.selectedResourceNode.uid,
        this.selectedResourceNode.namespace,
        this.selectedResourceNode.name).subscribe((argoEvents) => this.events = argoEvents.items);
    }
  }

  private iconForKind(kind: string) {
    switch (kind) {
      case 'Deployment':
        return 'pi pi-fw pi-refresh';
      case 'ReplicaSet':
        return 'pi pi-fw pi-copy';
      case 'Pod':
        return 'pi pi-fw pi-box';
      case 'Service':
        return 'pi pi-fw pi-window-maximize';
      case 'Ingress':
        return 'pi pi-fw pi-sign-in';
      case 'SealedSecret':
        return 'pi pi-fw pi-lock';
      case 'Secret':
        return 'pi pi-fw pi-unlock';
      case 'ConfigMap':
        return 'pi pi-fw pi-list';
      case 'PersistentVolumeClaim':
        return 'pi pi-fw pi-database';
      case 'Namespace':
        return 'pi pi-fw pi-stop';
      default:
        return undefined;
    }
  }

  eventsDescription(): string {
    if (this.selectedResourceNode && !this.selectedResourceNode.root) {
      return `${this.selectedResourceNode.kind} - ${this.selectedResourceNode.name}`;
    }
    return '';
  }

  reload() {
    this.getResources(this.environmentRelease.id.environmentId, this.environmentRelease.id.releaseId);
  }

  done() {
    this.location.back();
  }

  podNodeSelected(): boolean {
    return this.selectedResourceNode && this.selectedResourceNode.kind === 'Pod';
  }

  onTabChange(event: any) {
    if (event.index === 1 && this.podNodeSelected()) {
      // Logs tab
      this.containerTabActiveIndex = 0;
      this.releaseManageService.resource(this.environmentRelease.id,
        this.selectedResourceNode.namespace,
        this.selectedResourceNode.name,
        this.selectedResourceNode.version,
        this.selectedResourceNode.kind)
        .subscribe((resource) => {
            this.podManifest = JSON.parse(resource.manifest);
            this.containers = this.podManifest.spec.containers;
          }
        );
    }
  }
}
