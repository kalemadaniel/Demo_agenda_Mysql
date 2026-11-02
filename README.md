# 📅 Agenda Swing

Application de gestion de rendez-vous développée en **Java Swing**, avec le thème **Nimbus** et une base de données **MySQL**.

## ✨ Fonctionnalités

- Calendrier mensuel avec des semaines commençant le lundi.
- Navigation entre les mois et retour à la date du jour.
- Consultation des rendez-vous par journée.
- Ajout, modification et suppression de rendez-vous.
- Recherche par titre, lieu ou notes dans le mois affiché.
- Validation des dates et des horaires.
- Affichage de l’état de connexion à MySQL.
- Interface en français.
- Accès à la base en arrière-plan pour préserver la réactivité.

## 🛠️ Technologies

| Technologie | Utilisation |
| --- | --- |
| Java 8 ou supérieur | Développement de l’application |
| Swing | Interface graphique |
| Nimbus | Apparence des composants |
| MySQL | Stockage des rendez-vous |
| JDBC | Communication avec la base |
| NetBeans / Ant | Environnement de développement |

## 📋 Prérequis

- **Java 8 ou supérieur** pour exécuter l’application.
- Un **JDK** pour compiler le projet.
- Un serveur **MySQL** accessible.

> Le connecteur fourni est MySQL Connector/J **5.1.23**. Vérifiez sa compatibilité avec la version et l’authentification de votre serveur.

## 🚀 Installation

### 1. Télécharger le projet

Téléchargez le dépôt depuis GitHub, puis décompressez-le.

Ouvrez un terminal dans le dossier du projet.

### 2. Créer la base de données

Exécutez le script suivant dans MySQL Workbench, phpMyAdmin ou le client MySQL :

```text
database/schema.sql
```

Ce script crée :

- La base `java_calendar`.
- La table `appointment`.
- La vue de compatibilité `v_calendrier`.

> Le script crée la structure de la base. Il ne restaure pas les anciennes données et ne migre pas une table existante de structure différente.

### 3. Configurer MySQL

Copiez le fichier de configuration :

```powershell
Copy-Item database.properties.example database.properties
```

Modifiez `database.properties` avec vos paramètres :

```properties
url=jdbc:mysql://localhost:3306/java_calendar?connectTimeout=4000&socketTimeout=8000&useUnicode=true&characterEncoding=UTF-8
user=votre_utilisateur
password=votre_mot_de_passe
```

Vous pouvez également utiliser ces variables d’environnement, prioritaires sur le fichier :

```text
AGENDA_DB_URL
AGENDA_DB_USER
AGENDA_DB_PASSWORD
```

> Ne publiez jamais vos identifiants. Le fichier `database.properties` est exclu du suivi Git par `.gitignore`.

### 4. Compiler

Sous Windows :

```powershell
powershell -ExecutionPolicy Bypass -File .\Construire.ps1
```

### 5. Lancer

```powershell
.\Lancer.cmd
```

Si le dossier `dist` contient déjà l’application compilée, vous pouvez lancer directement `Lancer.cmd`.

### Avec NetBeans

1. Ouvrez le dossier du projet.
2. Vérifiez la configuration du JDK.
3. Sélectionnez la classe principale `kalema.collection.Agenda`.
4. Compilez et exécutez l’application.

## 📖 Utilisation

Sélectionnez un jour pour consulter ses rendez-vous dans le panneau de droite.

Cliquez sur **Nouveau rendez-vous** pour ajouter un événement.

| Champ | Description |
| --- | --- |
| Titre | Intitulé obligatoire |
| Date | Format `jj/mm/aaaa` |
| Début | Format `hh:mm` |
| Fin | Format `hh:mm` |
| Lieu | Emplacement ou lien de réunion |
| Notes | Informations complémentaires |

L’heure de fin doit être postérieure à l’heure de début, au cours de la même journée.

Les boutons **Modifier** et **Supprimer** sont disponibles sur chaque rendez-vous.

### Raccourcis clavier

| Raccourci | Action |
| --- | --- |
| `Ctrl + N` | Ajouter un rendez-vous |
| `F5` | Actualiser les données et la connexion |

## 🔌 Dépannage

Si la connexion MySQL est indisponible :

1. Vérifiez que le serveur MySQL est démarré.
2. Vérifiez que le script SQL a été exécuté.
3. Contrôlez l’adresse, l’utilisateur et le mot de passe.
4. Vérifiez la compatibilité du connecteur JDBC.
5. Cliquez sur **Actualiser**.

> Les rendez-vous sont enregistrés dans MySQL. Aucun stockage hors connexion n’est disponible.

## ✅ Vérifications

- Compilation Java et affichage Swing vérifiés.
- Ajout, lecture, modification et suppression testés sur HSQLDB en mémoire.
- Accents, apostrophes et validation des horaires vérifiés.
- Connexion à un serveur MySQL réel à valider dans l’environnement d’installation.

Consultez `VERIFICATIONS.txt` pour le détail des vérifications.
