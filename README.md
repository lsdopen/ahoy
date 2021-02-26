# Ahoy

![CI](https://github.com/lsdopen/ahoy/workflows/CI/badge.svg)

## Installation on Kubernetes

Ahoy relies on [Helm](https://helm.sh/docs/intro/install/) to be installed.

### Installation on Kubernetes

Add helm repo:
```shell
helm repo add lsdopen https://lsdopen.github.io/charts
helm repo update
```

Customise the Ahoy installation by editing the values in values-k8s.yaml.

Example value files available at: https://github.com/lsdopen/charts/tree/master/charts/ahoy

We are now ready to install Ahoy:
```shell
helm install ahoy --namespace ahoy --create-namespace --values values-k8s.yaml --devel lsdopen/ahoy
```

Note for GKE installation; you need to create a TLS secret and supply the secret name in the values file:
```shell
kubectl create secret tls ahoy-tls-secret -n ahoy --cert ahoy.crt --key ahoy.key
```

Hint: to create a self-signed certificate if you don't already have a certificate:
```shell
openssl req -newkey rsa:2048 -nodes -keyout ahoy.key -x509 -days 365 -out ahoy.crt
```

### Installation on OpenShift

Add helm repo:
```shell
helm repo add lsdopen https://lsdopen.github.io/charts
helm repo update
```

Create Ahoy project:
```shell
oc new-project ahoy --display-name="Ahoy" --description="Ahoy, your Kubernetes release management tool"
```

Customise the Ahoy installation by editing the values in values-ocp.yaml.

Example value files available at: https://github.com/lsdopen/charts/tree/master/charts/ahoy

OpenShift restricts the use of UID by default. 

To allow Postgres and Keycloak to start up you will need allow the anyuid SCC:
```shell
oc adm policy add-scc-to-user anyuid -z default -n ahoy
oc adm policy add-scc-to-user anyuid -z ahoy-keycloak -n ahoy
```

We are now ready to install Ahoy:
```shell
helm install ahoy --namespace ahoy --values values-ocp.yaml --devel lsdopen/ahoy
```

## Uninstall

To delete Ahoy:
```shell
helm delete ahoy --namespace ahoy
```

## Login

Ahoy makes use of [Keycloak](https://www.keycloak.org/) for authentication and authorisation. 

Use the default user credentials to log in for the first time:
```text
Username: ahoy
Password: ahoy
```
Note: on first login you'll be required to update the password for the default ahoy user.

## Setup

Ahoy makes use of Git, ArgoCD and Sealed Secrets to manage releases to clusters.
These need to be installed and setup before you'll be able to manage releases.

Once installed, configure their settings from the Ahoy UI on the Settings page. 

### Git

Configure Ahoy to use your own git repository by entering the repository and authentication details.
An initial commit is required for the repository to be used.

For SSH authentication, please note to generate the key pair using the PEM format: 
```shell
ssh-keygen -t rsa -m PEM
```

Some default known hosts will be added for you, but if you're using your own git repository, 
add SSH known host for your repository:
```shell
ssh-keyscan -t rsa <your repo: example github.com>
```

Add the output to the SSH Known Hosts field, for example for github:
```text
github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
```

### ArgoCD

Generate token for ahoy account:
```shell
argocd login <argocd-host>:8080
argocd account generate-token --account ahoy
```

Enter ArgoCD server (this may be something like https://ahoy-argocd-server.ahoy.svc:443/)

Enter the token generated above.

### Docker Registries

Add any private docker registries where your application images may reside.

### Sealed Secrets

If you require Ahoy to manage more than one cluster, export the keys to be used on subsequent clusters:
```shell
kubectl -n kube-system get secrets sealed-secrets-key***** -o yaml > sealed-secret.keys
```

## Adding new clusters

A default built-in cluster named `in-cluster` will automatically be added to Ahoy and ArgoCD.
For each new cluster you would like to manage, setup the following:

### Clusters

Each cluster you'd like to manage with Ahoy needs to be added under Clusters.

Add a new cluster and enter the type, name, master url, host and authentication details. 

Your `kubectl` context needs to be setup for the current cluster you're adding.

To get the master url:
```shell
kubectl cluster-info
```

The host is the suffix that Ahoy uses to suggest an ingress/route path for each application that is deployed to this cluster.

Refer to Kubernetes and OpenShift sections to create a ahoy service account and retrieve the token.

Getting CA Certificate from kubectl:
```shell
kubectl config view --raw --minify --flatten -o jsonpath='{.clusters[].cluster.certificate-authority-data}' | base64 --decode 
```

#### ArgoCD

Each new cluster also needs to be added to ArgoCD:
```shell
argocd login <argocd-host>:8080
argocd cluster add $(kubectl config current-context)
```

#### Sealed Secrets

For every new cluster that Ahoy manages, we need to use the same keys that were generated during setting up the first cluster.

Import the keys:
```shell
kubectl create -f sealed-secret.keys -n kube-system
```

[Install Sealed Secrets CRD and controller](https://github.com/bitnami-labs/sealed-secrets/releases)

### Kubernetes

Ahoy requires a service account to manage the Kubernetes cluster, to create this service account and get a token for the service account, follow these instructions:

```shell
kubectl create serviceaccount -n ahoy ahoy
kubectl create clusterrolebinding ahoy --clusterrole cluster-admin --serviceaccount=ahoy:ahoy
kubectl describe secrets -n ahoy ahoy-token-*****
```

### OpenShift

Ahoy requires a service account to manage the OpenShift cluster, to create this service account and get a token for the service account, follow these instructions:

```shell
oc create serviceaccount ahoy -n ahoy
oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:ahoy:ahoy
oc serviceaccounts get-token -n ahoy ahoy
```

## Notes

### Minikube

If using minikube, you'll need to enable the ingress controller in order for ingress to work:

`minikube addons enable ingress`

In order for DNS hosts to work, enable the ingress-dns addon:

`minikube addons enable ingress-dns`

[Setup OS to resolve the hosts](https://github.com/kubernetes/minikube/tree/master/deploy/addons/ingress-dns)
