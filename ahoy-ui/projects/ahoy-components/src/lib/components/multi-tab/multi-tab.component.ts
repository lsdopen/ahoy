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

import {AfterContentChecked, ChangeDetectorRef, Component, Input, OnInit, TemplateRef} from '@angular/core';

@Component({
  selector: 'app-multi-tab',
  templateUrl: './multi-tab.component.html',
  styleUrls: ['./multi-tab.component.scss']
})
export class MultiTabComponent implements OnInit, AfterContentChecked {
  @Input() content: TemplateRef<any>;
  @Input() defaultItem: object;
  @Input() items: object[];
  @Input() itemFactory: TabItemFactory<object>;
  @Input() deleteDisabled: (item) => boolean;
  @Input() deleteDisabledTooltip: (item) => string;
  @Input() itemIcon: (item) => string;
  indexes: number[] = [];
  indexCount = 0;
  selectedIndex = 0;

  constructor(private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    for (const item of this.items) {
      this.indexes.push(this.indexCount++);
    }
  }

  ngAfterContentChecked(): void {
    this.cd.detectChanges();
  }

  add() {
    const item = this.itemFactory();
    this.items.push(item);
    this.indexes.push(this.indexCount++);
    setTimeout(() => this.selectLastTab());
  }

  delete() {
    this.items.splice(this.selectedIndexN(), 1);
    this.indexes.splice(this.selectedIndexN(), 1);
    setTimeout(() => {
      if (this.selectedIndexN() === this.items.length) {
        // only move one tab back if it's the last tab
        this.selectLastTab();
      }
    });
  }

  isDeleteDisabled(): boolean {
    if (this.deleteDisabled) {
      if (this.defaultItem && this.selectedIndex === 0) {
        return this.deleteDisabled(this.defaultItem);
      }
      const item = this.items[this.selectedIndexN()];
      if (item) {
        return this.deleteDisabled(item);
      }
    }
    return false;
  }

  getDeleteDisabledTooltip(): string {
    if (this.deleteDisabled && this.deleteDisabledTooltip) {
      if (this.defaultItem && this.selectedIndex === 0) {
        return this.deleteDisabledTooltip(this.defaultItem);
      }
      const item = this.items[this.selectedIndexN()];
      if (item && this.deleteDisabled(item)) {
        return this.deleteDisabledTooltip(item);
      }
    }
    return '';
  }

  private selectLastTab() {
    if (this.defaultItem) {
      this.selectedIndex = this.items.length;
    } else {
      this.selectedIndex = this.items.length - 1;
    }
  }

  private selectedIndexN(): number {
    if (this.defaultItem) {
      return this.selectedIndex - 1;
    }
    return this.selectedIndex;
  }
}

export type TabItemFactory<T> = () => T;
