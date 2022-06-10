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

export class ResourceNode {
  name: string;
  kind: string;
  namespace: string;
  uid: string;
  version: string;
  parentUid: string;
  children: ResourceNode[] = undefined;
  root: boolean;
  leaf: boolean;
}

export class Resource {
  manifest: string;
}

export class ArgoEvents {
  items: Event[];
}

export class Event {
  action: string;
  count: number;
  eventTime: Date;
  firstTimestamp: Date;
  lastTimestamp: Date;
  message: string;
  reason: string;
  source: Source;
  involvedObject: InvolvedObject;
  type: string;
}

export class Source {
  component: string;
  host: string;
}

export class InvolvedObject {
  apiVersion: string;
  fieldPath: string;
  kind: string;
  name: string;
  namespace: string;
  resourceVersion: string;
  uid: string;
}
