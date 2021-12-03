# Users and Roles

Ahoy ships with some default users and roles.

## Users

| Username			| Default password 	| Role 				|
|-------------------|-------------------|-------------------|
| admin				| admin 			| admin				|
| releasemanager 	| releasemanager 	| releasemanager	|
| developer 		| developer 		| developer			|
| ahoy 				| ahoy 				| user				|

## Roles

| Role				| Description																						|
|-------------------|---------------------------------------------------------------------------------------------------|
| admin				| An Administrator is able to perform all functions 												|
| releasemanager 	| A Release Manager is able to perform all application, release and environment functions 			|
| developer 		| A Developer is able to define new releases and applications, as well as configure applications 	|
| user 				| The Ahoy user is able to view the status of releases but is unable to perform any functions		|

## Role matrix

| Primary function	| Secondary function	| Action 		| Administrator	| Release Manager	| Developer	| User	|
|-------------------|-----------------------|---------------|---------------|-------------------|-----------|-------|
| Dashboard			| 						| view			| X				| X					| X			| X		|
| Releases			|						| read			| X				| X					| X			| X		|
| 					|						| create/update	| X				| X					| X			| 		|
| 					|						| delete		| X				| X					| 			| 		|
| Release Manage	|						| view			| X 			| X					| X			| X		|
|					|						| deploy		| X 			| X					| 			| 		|
|					|						| undeploy		| X 			| X					| 			| 		|
|					|						| promote		| X 			| X					| 			| 		|
|					|						| upgrade		| X 			| X					| 			| 		|
|					|						| rollback		| X 			| X					| 			| 		|
|					| Manage Applications	| all			| X 			| X					| 			| 		|
|					| 						| env config	| X 			| X					| X			| 		|
| Release History	|						| view			| X 			| X					| X			| X		|
| Environments		|						| all			| X 			| X					| 			| 		|
| Applications		|						| all			| X 			| X					| X			| 		|
| Clusters			|						| all			| X 			| 					| 			| 		|
| Settings			| Git					| all			| X 			| 					| 			| 		|
| 					| Argo					| all			| X 			| 					| 			| 		|
| 					| Docker Registries		| all			| X 			| X					| X			| 		|
