
# lixbox-registry

Ce dépôt contient le µservice d'enregistrement des services.
Le site du service est [ici](https://project-site.portail.dev.lan/lixbox-registry)  

La convention de nommage des enregistrements est la suivante:


    {global; nom}-{admin; service}-{api;ui;admin}-{nom du service}


Exemple:

*   global-service-api-redis
*   referentiel-service-api-imdg-produit
*   seamax-service-ui-dashboard
 

## Dépendances
### API
* Nécessite un service Redis. 

### UI
* Sans objet

## Configuration 
### API
Les variables d'environnement suivantes servent à configurer le service:
* **REGISTRY_REDIS_URI**: URI du service Redis initialisée avec **tcp://localhost:6379**
* **QUARKUS_HTTP_PORT**: Port exposée par le service initialisé avec **18100**

### UI
Les variables d'environnement suivantes servent à configurer le service:
* **REGISTRY_API_URL**: URI du service Redis initialisée avec **http://localhost:18100/registry/api/1.0**
* **IAM_API_URL**: Port exposée par le service initialisé avec **http://localhost:18100/auth/api/1.0**  
     

## Utilisateur nécessaire

Sans objet


## Profil accepté par défaut

Sans objet


## Rôles disponibles pour le(s) service(s)

Sans objet

## Contrat et URL
### API

### UI



## Fonctions d'administration

Sans objet
     

## Fonctions batch

Sans objet


## Policies JAAS

Sans objet


## Database

Sans objet