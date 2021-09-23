# Notes

## Minikube

If using minikube, you'll need to enable the ingress controller in order for ingress to work:

`minikube addons enable ingress`

Note: SSL pass through is not enabled by default for the nginx ingress controller. This needs to be enabled manually by editing the deployment:

```shell
kubectl edit deployment ingress-nginx-controller -n kube-system
```

Add an extra argument under containers -> args:

```shell
 - --enable-ssl-passthrough=true
```

Save and wait for the ingress controller pod to restart.

In order for DNS hosts to work, enable the ingress-dns addon:

`minikube addons enable ingress-dns`

[Setup OS to resolve the hosts](https://github.com/kubernetes/minikube/tree/master/deploy/addons/ingress-dns)
