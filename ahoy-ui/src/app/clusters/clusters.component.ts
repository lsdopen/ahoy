import {Component, OnInit} from '@angular/core';
import {Cluster} from './cluster';
import {ActivatedRoute} from '@angular/router';
import {ClusterService} from './cluster.service';
import {LoggerService} from '../util/logger.service';
import {Confirmation} from '../components/confirm-dialog/confirm';
import {filter} from 'rxjs/operators';
import {DialogService} from '../components/dialog.service';

@Component({
  selector: 'app-clusters',
  templateUrl: './clusters.component.html',
  styleUrls: ['./clusters.component.scss']
})
export class ClustersComponent implements OnInit {
  clusters: Cluster[] = undefined;

  constructor(private route: ActivatedRoute,
              private clusterService: ClusterService,
              private log: LoggerService,
              private dialogService: DialogService) {
  }

  ngOnInit() {
    this.getAllClusters();
  }

  private getAllClusters() {
    this.log.debug('getting all clusters');
    this.clusterService.getAll()
      .subscribe(clusters => this.clusters = clusters);
  }

  delete(cluster: Cluster) {
    const confirmation = new Confirmation(`Are you sure you want to delete ${cluster.name}?`);
    confirmation.verify = true;
    confirmation.verifyText = cluster.name;
    this.dialogService.showConfirmDialog(confirmation).pipe(
      filter((conf) => conf !== undefined)
    ).subscribe(() => {
      this.clusterService.destroy(cluster)
        .subscribe(() => this.getAllClusters());
    });
  }
}
