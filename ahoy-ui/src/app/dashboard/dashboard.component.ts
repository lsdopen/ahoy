import {Component, OnInit} from '@angular/core';
import {EnvironmentService} from '../environments/environment.service';
import {Environment} from '../environments/environment';
import {Cluster} from '../clusters/cluster';
import {ClusterService} from '../clusters/cluster.service';
import {ActivatedRoute} from '@angular/router';
import {LoggerService} from '../util/logger.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  selectedCluster: Cluster;
  environments: Environment[] = undefined;
  clusters: Cluster[] = undefined;

  constructor(
    private route: ActivatedRoute,
    private clusterService: ClusterService,
    private environmentService: EnvironmentService,
    private log: LoggerService) {
  }

  ngOnInit() {
    const clusterId = +this.route.snapshot.queryParamMap.get('clusterId');

    this.clusterService.getAll()
      .subscribe((clusters) => {
        this.clusters = clusters;

        if (clusterId === 0) {
          this.clusterService.getLastUsedId().subscribe((lastUsedClusterId) => {
            if (lastUsedClusterId !== 0) {
              this.getEnvironments(lastUsedClusterId);
            }
          });
        } else {
          this.getEnvironments(clusterId);
        }
      });
  }

  private getEnvironments(clusterId: number) {
    this.log.debug('getting environments for clusterId=', clusterId);

    this.clusterService.get(clusterId)
      .subscribe(cluster => {
        this.selectedCluster = cluster;
        this.environmentService.getAllEnvironmentsByCluster(clusterId)
          .subscribe(envs => this.environments = envs);
      });
  }

  compareClusters(c1: Cluster, c2: Cluster): boolean {
    if (c1 === null) {
      return c2 === null;
    }

    if (c2 === null) {
      return c1 === null;
    }

    return c1.id === c2.id;
  }

  clusterChanged() {
    this.getEnvironments(this.selectedCluster.id);
  }
}
