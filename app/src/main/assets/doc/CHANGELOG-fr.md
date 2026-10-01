******

### Historique des versions

******

# v1.0.0

###### 2026/10/01

* `Note` Aperçu de développement P2: installation, inspection, requêtes utilisateur et désinstallation sont reliées au service hôte, avec confirmation explicite et nettoyage automatique des sessions. La validation complète des entrées hôtes, l'interface complète, l'ouverture externe, l'activation comme installateur par défaut, l'API de script et les paramètres restent en cours.
* `Fonctionnalité` Identité du plugin `three-setup-installer` (engine `installer`) avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.INSTALLER` pour la découverte par l'hôte
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Amélioration` P0 a validé l'installation silencieuse, les mises à jour, la désinstallation et le choix ordinaire de l'installateur par défaut avec Shizuku et Root. Les points d'installation depuis l'hôte et les scripts ne sont pas encore disponibles; les valeurs par défaut persistantes restent hors de cette version.
* `Amélioration` L'identifiant du plugin, le moteur, l'action / la categorie du service, le descripteur Binder et la version minimale de l'hote proviennent desormais des constantes du contrat installer-api de l'hote; les capacites declarent la version 1 du contrat d'installation et la build minimale de l'hote est fixee a 5299
* `Amélioration` Les sources à accès aléatoire évitent une copie complète en cache, tandis que les flux sont stockés temporairement si nécessaire. Les paquets fractionnés en ZIP sont pris en charge, les AAB restent limités à l'inspection et les sources modifiées sont refusées.
* `Amélioration` Le mode d'autorisation choisi explicitement ne bascule jamais vers un autre. Le refus, le délai dépassé et l'incompatibilité sont distingués, et les requêtes simultanées partagent l'autorisation et les connexions privilégiées.
* `Amélioration` Le moteur d'installation et de mise à jour utilise la confirmation système, Shizuku ou Root, permet l'annulation et indique le mode de confirmation réel et la réponse du système.
* `Amélioration` Le moteur de désinstallation prend en charge la confirmation système, Shizuku et Root, avec conservation facultative des données en mode privilégié.
* `Amélioration` L'installation séquentielle par lot peut continuer après un échec ou annuler les éléments restants, avec validation et sélection des utilisateurs cibles en mode privilégié.
* `Amélioration` Le service hôte permet l'inspection, l'installation, la désinstallation et la consultation des utilisateurs, avec confirmation explicite, annulation à la fermeture de l'appelant, quatre sessions simultanées au maximum et nettoyage automatique.
* `Amélioration` Les dialogues suivent la langue, le mode nuit et la couleur d'AutoJs6, avec un repli sans hôte et une présentation adaptée au texte agrandi et au RTL.
* `Dépendance` Ajout de Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) pour le mode d'autorisation Shizuku
* `Dépendance` Ajout de libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) pour le mode d'autorisation Root
* `Dépendance` Ajout de AndroidHiddenApiBypass 6.1 pour les API cachées de l'installateur de paquets utilisées par le service privilégié
* `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5298, MPL 2.0) comme contrat de plugin partagé, avec hachage verrouillé dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de `package-archive-parser.aar` et `installer-api.aar` (modules AutoJs6 `plugin-api/package-archive-parser` et `plugin-api/installer-api`, build hote P1 6.8.0 / 5299, MPL 2.0), avec hachage verrouille dans `locks/host-api-aars.lock` aux cotes de `common-plugin-api.aar`
* `Dépendance` Mise à jour de l'analyseur de paquets intégré pour reconnaître les conteneurs ZIP ordinaires avec APK fractionnés
