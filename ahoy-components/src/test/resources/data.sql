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

INSERT INTO PUBLIC.SETTINGS (TYPE, SETTINGS)
VALUES ('GIT', '{"type":"GIT","remoteRepoUri":"","branch":"master","credentials":"NONE","httpsUsername":null,"httpsPassword":null,"privateKey":"repo-private-key"}');
INSERT INTO PUBLIC.SETTINGS (TYPE, SETTINGS)
VALUES ('ARGO', '{"type":"ARGO","argoServer":"https://argocd.minikube.host:443/","argoToken":"argo-token"}');
INSERT INTO PUBLIC.SETTINGS (TYPE, SETTINGS)
VALUES ('DOCKER', '{"type":"DOCKER","dockerRegistries":[{"name":"my-docker-hub","server":"https://docker.io/","username":"docker-server","password":"docker-password","secure":true}]}');

INSERT INTO PUBLIC.CLUSTER (ID, NAME, MASTER_URL, HOST, IN_CLUSTER)
VALUES (1, 'minikube', 'https://minikube:8443', 'minikube.host', true);
