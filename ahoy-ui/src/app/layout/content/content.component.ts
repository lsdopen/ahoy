import {Component, Input, OnInit, TemplateRef} from '@angular/core';

@Component({
  selector: 'app-content',
  templateUrl: './content.component.html',
  styleUrls: ['./content.component.scss']
})
export class ContentComponent implements OnInit {
  @Input() title: TemplateRef<any>;
  @Input() buttonBar: TemplateRef<any>;

  constructor() {
  }

  ngOnInit() {
  }

}
