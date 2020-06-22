import {Component, OnInit} from '@angular/core';
import {Cluster} from '../cluster';
import {ActivatedRoute} from '@angular/router';
import {Location} from '@angular/common';
import {LoggerService} from '../../util/logger.service';
import {ClusterService} from '../cluster.service';

@Component({
  selector: 'app-cluster-detail',
  templateUrl: './cluster-detail.component.html',
  styleUrls: ['./cluster-detail.component.scss']
})
export class ClusterDetailComponent implements OnInit {
  types = [
    {value: 'OPENSHIFT', viewValue: 'Openshift'},
    {value: 'KUBERNETES', viewValue: 'Kubernetes'},
    {value: 'NOOP', viewValue: 'Noop'}
  ];

  cluster: Cluster;
  hideToken = true;

  constructor(private route: ActivatedRoute,
              private clusterService: ClusterService,
              private location: Location,
              private log: LoggerService) {
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id === 'new') {
      this.cluster = new Cluster();

    } else {
      this.clusterService.get(+id)
        .subscribe(cluster => this.cluster = cluster);
    }
  }

  save() {
    this.clusterService.save(this.cluster)
      .subscribe(() => this.location.back());
  }

  cancel() {
    this.cluster = undefined;
    this.location.back();
  }
}
