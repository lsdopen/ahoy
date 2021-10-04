# Additional cluster setup

A default built-in cluster named `in-cluster` will automatically be added to Ahoy and ArgoCD.

For each new cluster you would like to manage, setup the following:

## Clusters

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

## ArgoCD

Each new cluster also needs to be added to ArgoCD:

```shell
argocd login <argocd-host>:8080
argocd cluster add $(kubectl config current-context)
```

## Sealed Secrets

For every new cluster that Ahoy manages, we need to use the same keys that were generated during setting up the first cluster.

Export the keys from initial cluster:

```shell
kubectl -n kube-system get secrets sealed-secrets-key***** -o yaml > sealed-secret.keys
```

Import the keys:

```shell
kubectl create -f sealed-secret.keys -n kube-system
```

[Install Sealed Secrets CRD and controller](https://github.com/bitnami-labs/sealed-secrets/releases)

## Kubernetes

Ahoy requires a service account to manage the Kubernetes cluster, to create this service account and get a token for the service account, follow these instructions:

```shell
kubectl create serviceaccount -n ahoy ahoy
kubectl create clusterrolebinding ahoy --clusterrole cluster-admin --serviceaccount=ahoy:ahoy
kubectl describe secrets -n ahoy ahoy-token-*****
```

## OpenShift

Ahoy requires a service account to manage the OpenShift cluster, to create this service account and get a token for the service account, follow these instructions:

```shell
oc create serviceaccount ahoy -n ahoy
oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:ahoy:ahoy
oc serviceaccounts get-token -n ahoy ahoy
```
