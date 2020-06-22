# Ahoy

## Installation on Kubernetes

Ahoy relies on Helm version 3.

TL;DR installation of Helm

```
wget https://get.helm.sh/helm-v3.2.1-linux-amd64.tar.gz
tar zxvf helm-v3.2.1-linux-amd64.tar.gz
sudo mv linux-amd64/helm /usr/local/bin/
rm helm-v3.2.1-linux-amd64.tar.gz linux-amd64/ -rf
```

### Installation on Kubernetes

Clone git repo
```
git clone https://gitlab.lsd.co.za/lsd/boost.git
cd /ops/charts/ahoy
```

Create Ahoy namespace
```
kubectl create namespace ahoy
```

Customise the Ahoy installation by editing the values in values-k8s.yaml.

Ahoy depends on Postgres, so we are going to install Posgres while the Helm chart

```
helm dependency update
```

We are now ready to install Ahoy
```
helm install ahoy --namespace ahoy --values values-k8s.yaml .
```

Note for GKE installation: you need to create a TLS secret and supply the secret name in the values file.
{: .alert .alert-gitlab-orange}
```
kubectl create secret tls ahoy-tls-secret -n ahoy --cert ahoy.crt --key ahoy.key
```

Hint: to create a self-signed certificate if you don't already have a certificate:

```
openssl req -newkey rsa:2048 -nodes -keyout ahoy.key -x509 -days 365 -out ahoy.crt
```

### Installation on Openshift

Clone git repo
```
git clone https://gitlab.lsd.co.za/lsd/boost.git
cd /ops/charts/ahoy
```

Create Ahoy project
```
oc new-project ahoy --display-name="Ahoy" --description="Ahoy, your Kubernetes release management tool"
```

Customise the Ahoy installation by editing the values in values-ocp.yaml.

Ahoy depends on Postgres, so we are going to install Posgres while the Helm chart

```
helm dependency update
```

Openshift restrict the use of UID by default. To allow Postgres to start up you will need allow the anyuid SCC
```
oc adm policy add-scc-to-user anyuid -z default -n ahoy
```

We are now ready to install Ahoy
```
helm install ahoy --namespace ahoy --values values-ocp.yaml .
```

## Uninstall

To delete Ahoy:

`helm delete ahoy --namespace ahoy`

### Sealed Secrets

[Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) is required for Ahoy to commit secrets to git:

[Install Sealed Secrets CRD and controller](https://github.com/bitnami-labs/sealed-secrets/releases)

If you require Ahoy to manage more than one cluster, export the keys to be used on subsequent clusters:

`kubectl -n kube-system get secrets sealed-secrets-key***** -o yaml > sealed-secret.keys`

## Setup

Ahoy makes use of Git and ArgoCD to manage releases to clusters.
These need to be setup before you'll be able to manage releases. 
Do this by configuring their settings from the Ahoy UI on the Settings page. 

### Git

Configure Ahoy to use your own git repository by entering the repository and authentication details.
An initial commit is required for the repository to be used.

For SSH authentication, please note to generate the key pair using the PEM format: 

`ssh-keygen -t rsa -m PEM`

Add SSH known host for your repository:

`ssh-keyscan -t rsa <your repo: example github.com>`

Add the output to the SSH Known Hosts field, for example for github:

```
github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
```

### ArgoCD

[Install ArgoCD](https://argoproj.github.io/argo-cd/getting_started)

Add Ahoy user to argocd:

`kubectl edit cm -n argocd argocd-cm`

Add:
```yaml
data:
  accounts.ahoy: apiKey,login
```

Add role for ahoy user:

`kubectl edit cm -n argocd argocd-rbac-cm`

Add:
```yaml
data:
  policy.csv: |
    g, ahoy, role:admin
```

Generate token for ahoy account:
`argocd account generate-token --account ahoy`

Enter ArgoCD server (this may be something like https://argocd-server.argocd.svc:443/)

Enter the token generated above.

### Docker Registries

Add any private docker registries where your application images may reside.

## Adding new clusters

A default built-in cluster named `in-cluster` will automatically be added to Ahoy and ArgoCD. For each new cluster you would like to manage, setup the following:

### Clusters

Each cluster you'd like to manage with Ahoy needs to be added under Clusters.

Add a new cluster and enter the type, name, master url, host and authentication details. 

Your `kubectl` context needs to be setup for the current cluster you're adding.

To get the master url:

`kubectl cluster-info`

The host is the suffix that Ahoy uses to suggest an ingress/route path for each application that is deployed to this cluster.

Refer to Kubernetes and OpenShift sections to create a ahoy service account and retrieve the token.

Getting CA Certificate from kubectl:

`kubectl config view --raw --minify --flatten -o jsonpath='{.clusters[].cluster.certificate-authority-data}' | base64 --decode` 

#### ArgoCD

Each new cluster also needs to be added to ArgoCD:

`argocd cluster add $(kubectl config current-context)`

#### Sealed Secrets

For every new cluster that Ahoy manages, we need to use the same keys that were generated during setting up the first cluster.

Import the keys:

`kubectl create -f sealed-secret.keys -n kube-system`

[Install Sealed Secrets CRD and controller](https://github.com/bitnami-labs/sealed-secrets/releases)

## Notes

### Kubernetes

Ahoy requires a service account to manage the Kubernetes cluster, to create this service account and get a token for the service account, follow these instructions:

`kubectl describe secrets -n ahoy ahoy-token-*****`

### OpenShift

Ahoy requires a service account to manage the OpenShift cluster, to create this service account and get a token for the service account, follow these instructions:

`oc create serviceaccount ahoy -n default`

`oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:default:ahoy`

`oc serviceaccounts get-token -n default ahoy` 

### Minikube

If using minikube, you'll need to enable the ingress controller in order for ingress to work:

`minikube addons enable ingress`

In order for DNS hosts to work, enable the ingress-dns addon:

`minikube addons enable ingress-dns`

[Setup OS to resolve the hosts](https://github.com/kubernetes/minikube/tree/master/deploy/addons/ingress-dns)
