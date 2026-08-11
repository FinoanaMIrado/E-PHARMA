-- =====================================================================
--  APPLICATION DE GESTION D'UNE PHARMACIE
--  Script de creation de la base de donnees (MySQL / MariaDB)
--
--  Utilisation :
--      mysql -u root -p < src/main/resources/sql/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pharmacie
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE pharmacie;

-- ---------------------------------------------------------------------
--  Suppression dans l'ordre inverse des dependances
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS achat;
DROP TABLE IF EXISTS entree;
DROP TABLE IF EXISTS medicament;
DROP TABLE IF EXISTS utilisateur;

-- ---------------------------------------------------------------------
--  Table MEDICAMENT
--  Le stock initial est toujours 0 (regle 19.1 du cahier des charges).
--  Il est ensuite augmente par les entrees et diminue par les achats.
-- ---------------------------------------------------------------------
CREATE TABLE medicament (
    numMedoc      VARCHAR(20)  NOT NULL,
    Design        VARCHAR(150) NOT NULL,
    prix_unitaire INT          NOT NULL DEFAULT 0,
    stock         INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_medicament        PRIMARY KEY (numMedoc),
    CONSTRAINT chk_medicament_prix  CHECK (prix_unitaire >= 0),
    CONSTRAINT chk_medicament_stock CHECK (stock >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_medicament_design ON medicament (Design);

-- ---------------------------------------------------------------------
--  Table ENTREE
--  Une entree augmente le stock du medicament concerne.
-- ---------------------------------------------------------------------
CREATE TABLE entree (
    numEntree   VARCHAR(20) NOT NULL,
    numMedoc    VARCHAR(20) NOT NULL,
    stockEntree INT         NOT NULL,
    dateEntree  DATE        NOT NULL,
    CONSTRAINT pk_entree        PRIMARY KEY (numEntree),
    CONSTRAINT fk_entree_medoc  FOREIGN KEY (numMedoc)
        REFERENCES medicament (numMedoc)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_entree_qte   CHECK (stockEntree > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_entree_medoc ON entree (numMedoc);
CREATE INDEX idx_entree_date  ON entree (dateEntree);

-- ---------------------------------------------------------------------
--  Table ACHAT
--  Un meme achat (numAchat) peut contenir PLUSIEURS medicaments :
--  chaque ligne du panier est enregistree comme une ligne de cette table.
--  La cle primaire est donc le couple (numAchat, numMedoc).
--
--  DECISION DOCUMENTEE : la colonne prix_unitaire n'est pas listee dans
--  le cahier des charges. Elle est ajoutee ici pour figer le prix pratique
--  au moment de la vente. Sans cela, une modification ulterieure du prix
--  d'un medicament fausserait retroactivement les factures deja emises
--  ainsi que la recette totale et les recettes mensuelles.
-- ---------------------------------------------------------------------
CREATE TABLE achat (
    numAchat      VARCHAR(20)  NOT NULL,
    numMedoc      VARCHAR(20)  NOT NULL,
    nomClient     VARCHAR(150) NOT NULL,
    nbr           INT          NOT NULL,
    dateAchat     DATE         NOT NULL,
    prix_unitaire INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_achat       PRIMARY KEY (numAchat, numMedoc),
    CONSTRAINT fk_achat_medoc FOREIGN KEY (numMedoc)
        REFERENCES medicament (numMedoc)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_achat_qte  CHECK (nbr > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_achat_client ON achat (nomClient);
CREATE INDEX idx_achat_date   ON achat (dateAchat);
CREATE INDEX idx_achat_medoc  ON achat (numMedoc);

-- ---------------------------------------------------------------------
--  Table UTILISATEUR
--  DECISION DOCUMENTEE : le cahier des charges impose un ecran de login
--  mais ne definit aucune table d'utilisateurs. Cette table minimale est
--  donc ajoutee. Les mots de passe sont stockes hashes (BCrypt), jamais
--  en clair.
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id           INT          NOT NULL AUTO_INCREMENT,
    login        VARCHAR(50)  NOT NULL,
    motDePasse   VARCHAR(100) NOT NULL,
    nomComplet   VARCHAR(150) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'PHARMACIEN',
    CONSTRAINT pk_utilisateur  PRIMARY KEY (id),
    CONSTRAINT uk_utilisateur_login UNIQUE (login)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  Comptes livres avec l'application
--
--      login : admin    mot de passe : admin123     role : ADMIN
--      login : fimiri   mot de passe : qwerty1234   role : PHARMACIEN
--
--  ATTENTION : la colonne motDePasse contient un hachage BCrypt, jamais le
--  mot de passe en clair. L'application verifie les mots de passe avec
--  BCrypt.verify() : une valeur en clair inseree a la main ne pourra JAMAIS
--  etre validee, et la connexion echouera meme avec le bon mot de passe.
--
--  Pour ajouter un compte, generez d'abord le hachage :
--      System.out.println(pharmacie.service.AuthService.hacher("motDePasse"));
-- ---------------------------------------------------------------------
INSERT INTO utilisateur (login, motDePasse, nomComplet, role) VALUES
    ('admin',
     '$2a$12$WRXe3xFouqpRGHGmmdqeHOlObnn4JsQYXYmp88CTnYDg/v2nA6nxO',
     'Administrateur',
     'ADMIN'),
    ('fimiri',
     '$2a$12$3.uG9joRKwovt1NMfAdnYO6sAaJRum877qhFgfBRJWUEGc3YsP.me',
     'Fimiri',
     'PHARMACIEN');

