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

import {ParamMap} from '@angular/router';

export class ParamMapStub implements ParamMap {
  private params: Map<string, string>;
  readonly keys = [];

  constructor(para?: { [key: string]: string }) {
    this.setParams(para);
  }

  setParams(para?: { [key: string]: string }) {
    this.params = new Map<string, string>();
    for (const key of Object.keys(para)) {
      this.params.set(key, para[key]);
    }

    while (this.keys.length) {
      this.keys.pop();
    }
    this.keys.push(Object.keys(para));
  }

  get(name: string): string | null {
    return this.params.get(name);
  }

  getAll(name: string): string[] {
    return [this.params.get(name)];
  }

  has(name: string): boolean {
    return this.params.has(name);
  }
}
