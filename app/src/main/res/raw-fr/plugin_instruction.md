3-Setup Installer installe, met à jour, inspecte et désinstalle des applications Android depuis son accueil autonome, les entrées et scripts AutoJs6, ou l'ouverture et le partage de paquets par d'autres applications. Il prend en charge la confirmation Android et les opérations privilégiées via Shizuku ou Root.

1.0.0: Aperçu de développement avec accueil autonome, paramètres, gestion des applications installées, files séquentielles et historique d'installation. La confirmation, la progression, les résultats et les notifications au premier plan sont disponibles. Un redémarrage conserve les résultats confirmés enregistrés et marque les tâches inachevées comme annulées, sans reprise ni nouvelle tentative automatique. L'API de script `installer` nécessite AutoJs6 >= 6.8.0 (5300); l'intégration de base nécessite la version de compilation 5299. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour les appareils couverts et les validations restantes.

### Utilisation

1. Installez l'APK du plugin depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) sur Android 7.0 ou ultérieur. Son entrée dans le lanceur ouvre l'accueil autonome.
2. Sur l'accueil, vérifiez les autorisations et l'installateur par défaut, puis utilisez le bouton d'ajout pour sélectionner un ou plusieurs paquets. Vérifiez la boîte de dialogue avant de confirmer et suivez la progression et l'historique sur l'accueil.
3. Pour l'intégration AutoJs6, utilisez la version de compilation 5299 (6.8.0) ou ultérieure et activez `3-Setup Installer` dans le centre de plugins. L'API de script nécessite la version de compilation 5300 ou ultérieure.
4. Utilisez une action d'installation d'AutoJs6 ou choisissez 3-Setup Installer pour ouvrir ou partager des paquets. Si une confirmation apparaît, vérifiez l'application et les options avant d'installer. Préparez l'autorisation Shizuku ou Root si vous choisissez une méthode privilégiée.
5. Le menu d'accueil ouvre les applications installées et les paramètres. Vérifiez les réglages locaux d'installation, l'apparence, l'icône et les notifications; À propos, l'historique des versions et la recherche manuelle de mises à jour se trouvent dans les paramètres.

### Modes d'autorisation

- `none` : la session PackageInstaller standard ; Android demande à l'utilisateur de confirmer chaque installation, les paquets fractionnés sont pris en charge et les options privilégiées ne sont pas disponibles.
- `shizuku`: nécessite Shizuku en cours d'exécution (démarré par débogage sans fil, ADB ou Root) et la permission accordée au plugin. Les droits shell permettent l'installation et la désinstallation silencieuses, ainsi que les opérations pour d'autres utilisateurs.
- `root` : nécessite un gestionnaire Root qui accorde `su` au plugin ; offre les mêmes opérations que Shizuku via un service root libsu. La rétrogradation sur un firmware ordinaire (user) ne réussit toujours que pour les applications debuggable, ce qui est une règle du framework et non une limite du plugin.
- **Remarque:** Lorsque les privilèges sont disponibles, les demandes de l'hôte avec `interaction: 'auto'` installent silencieusement sans ouvrir d'abord une confirmation. Si Android exige une confirmation, `auto` l'autorise et la consigne dans `notes`. Utilisez `interaction: 'dialog'` pour confirmer avant l'installation, ou `interaction: 'silent'` pour échouer si une confirmation système est nécessaire. L'API de scripts suit le même comportement par défaut.
- Les paramètres enregistrent l'ordre et l'activation des autorisations, les options d'installation et les notifications de progression. Les installations locales et externes utilisent `dialog` par défaut; les choix `auto` ou `silent` enregistrés explicitement s'appliquent. Les requêtes hôte/script conservent leurs options explicites, et l'API de script garde `auto` par défaut. Les choix ne sont enregistrés qu'après confirmation.

### Permissions et sécurité

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul AutoJs6 peut donc les atteindre ; l'entrée externe "Ouvrir avec" n'accepte que des fichiers de paquet et n'exécute jamais de script.
- REQUEST_INSTALL_PACKAGES et REQUEST_DELETE_PACKAGES permettent la confirmation Android. QUERY_ALL_PACKAGES sert à gérer les applications installées, comparer versions et signatures, et détecter l'installateur par défaut.
- FOREGROUND_SERVICE et FOREGROUND_SERVICE_DATA_SYNC soutiennent l'installation en arrière-plan; POST_NOTIFICATIONS permet les notifications de progression et de résultats. L'absence de permission de notification ne bloque pas l'installation.
- Shizuku et Root ne servent qu'à l'opération que vous lancez ; le service privilégié ne conserve aucun état, ne garde aucun shell ouvert entre les opérations et n'est jamais atteint depuis l'extérieur du plugin.
- L'installation, l'inspection, l'historique et la gestion des applications fonctionnent hors ligne. INTERNET est utilisé uniquement lors d'une recherche manuelle de versions auprès de l'API GitHub Releases fixe du plugin, avec un intervalle de 12 heures. Aucune recherche en arrière-plan ni aucun envoi de paquets n'est effectué.
- Les sources sont ouvertes en lecture seule. L'historique conserve des métadonnées limitées et des résultats, sans contenu de paquet ni URI source; les chemins des erreurs sont masqués. Le stockage privé est exclu des sauvegardes. Supprimer une entrée ne désinstalle pas l'application et ne supprime pas sa source.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) pour le guide d'installation et l'avancement actuel.
