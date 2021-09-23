# Installation

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
