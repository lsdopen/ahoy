# Additional cluster setup

A default built-in cluster named `in-cluster` will automatically be added to Ahoy and ArgoCD.

For each new cluster you would like to manage, setup the following:

## Clusters

Each cluster you'd like to manage with Ahoy needs to be added under Clusters.

Add a new cluster and enter the name, master url and host.

Your `kubectl` context needs to be setup for the current cluster you're adding.

To get the master url:

```shell
kubectl cluster-info
```
Please note; the master url needs to match the cluster URL in ArgoCD in order to deploy releases to the cluster.

The host is the suffix that Ahoy uses to suggest an ingress/route path for each application that is deployed to this cluster.

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
