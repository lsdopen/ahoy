# Running Ahoy

In order to run Ahoy in development, some prerequisites are required, namely Keycloak, ArgoCD and Sealed Secrets.

## Ahoy helm chart

The easiest way to install these pre-requisites is to install the latest development version of the Ahoy helm chart
into a development Kubernetes environment, such as minikube.

```shell
helm repo add ahoy https://lsdopen.github.io/ahoy-helm
helm repo update
helm install ahoy --namespace ahoy --create-namespace --values values.yaml --devel ahoy/ahoy
```

## Run the UI

```shell
cd ahoy-ui
npm run start
```

## Run the Server

```shell
cd ahoy-server
mvn spring-boot:run
```

### Overriding run properties

You can override the run properties by supplying the property as a system property.

For example, if you'd like to move the db out of the target directory so that it is not deleted on `mvn clean` executions, then run the server with the following:
```shell
mvn spring-boot:run -Dapp.db.location=/path/to/db
```

### Keycloak certificate

When installing with the default chart values, the default Keycloak hostname is `keycloak.minikube.host` and the default run configuration should work,
all that is required is the `keycloak-tls` secret and values configuration (instructions below).

If your installation differs and the keycloak hostname is different, you can run the server by overriding the hostname:
```shell
mvn spring-boot:run -Dapp.keycloak.host=mykeycloak.host
```

Please note; you will encounter a `401` error if the server cannot retrieve the JWK set from Keycloak; which usually occurs because
a self-signed certificate has been issued for Keycloak's ingress which the server cannot verify.

In order to fix this, you need to generate a certificate, create a TLS secret from the certificate, configure Keycloak's ingress to use
the secret and add the certificate to Ahoy server's truststore.

Generate certificate; entering the details and correct common name (eg, fully qualified host name) for Keycloak:
```shell
openssl req -newkey rsa:2048 -nodes -keyout keycloak-minikube.key -x509 -days 365 -out keycloak-minikube.crt
```

Create TLS secret:
```shell
kubectl create secret -n ahoy tls keycloak-tls --cert keycloak-minikube.crt --key keycloak-minikube.key
```

Configure Keycloak ingress:
```yaml
keycloak:
  ingress:
    tls:
      - hosts:
          - keycloak.minikube.host
        secretName: "keycloak-tls"
```

Import certificate into Ahoy's truststore:
```shell
keytool -import -trustcacerts -alias keycloak-minikube -file keycloak-minikube.crt -keystore ahoy-truststore.jks -keypass changeit
```

## Setting up Ahoy

Setup ahoy as per these [instructions](./setup.md)
