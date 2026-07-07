-- ========== CLIENTS ==========
INSERT INTO client (id, numero_client, nom, prenom, type_client, email, telephone, adresse, ville, code_postal, segment, date_creation)
VALUES
    (1, 'CLI-2026-00001', 'Dupont', 'Marie', 'PARTICULIER', 'marie.dupont@email.fr', '+33612345678', '12 rue de la Paix', 'Paris', '75001', 'PREMIUM', NOW()),
    (2, 'CLI-2026-00002', 'Martin', 'Jean', 'PARTICULIER', 'jean.martin@email.fr', '+33698765432', '5 avenue Foch', 'Lyon', '69001', 'STANDARD', NOW()),
    (3, 'CLI-2026-00003', 'SCI Les Oliviers', NULL, 'ENTREPRISE', 'contact@sci-oliviers.fr', '+33491000000', '8 bd Michelet', 'Marseille', '13008', 'VIP', NOW()),
    (4, 'CLI-2026-00004', 'Ben Ammar', 'Karim', 'PARTICULIER', 'karim.benammar@email.fr', '+33561000000', '15 rue Alsace', 'Toulouse', '31000', 'STANDARD', NOW()),
    (5, 'CLI-2026-00005', 'Garage Leroy SARL', NULL, 'ENTREPRISE', 'contact@garage-leroy.fr', '+33320000000', '22 rue Nationale', 'Lille', '59000', 'PREMIUM', NOW()),
    (6, 'CLI-2026-00006', 'Petit', 'Sophie', 'PARTICULIER', 'sophie.petit@email.fr', '+33556000000', '3 cours Clemenceau', 'Bordeaux', '33000', 'STANDARD', NOW()),
    (7, 'CLI-2026-00007', 'Restaurant Chez Paul', NULL, 'ENTREPRISE', 'paul@chezpaul.fr', '+33493000000', '10 rue Paradis', 'Nice', '06000', 'STANDARD', NOW()),
    (8, 'CLI-2026-00008', 'Durand', 'Pierre', 'PARTICULIER', 'pierre.durand@email.fr', '+33240000000', '7 quai Brancas', 'Nantes', '44000', 'VIP', NOW()),
    (9, 'CLI-2026-00009', 'Moreau', 'Claire', 'PARTICULIER', 'claire.moreau@email.fr', '+33388000000', '14 rue Mercière', 'Strasbourg', '67000', 'PREMIUM', NOW()),
    (10, 'CLI-2026-00010', 'Cabinet Moreau et Fils', NULL, 'ENTREPRISE', 'contact@moreau-fils.fr', '+33144000000', '50 rue de Rivoli', 'Paris', '75004', 'VIP', NOW());

-- ========== CONTRATS ==========
INSERT INTO contrat (id, numero_contrat, produit, statut, date_effet, date_echeance, prime_annuelle, plafond_garantie, franchise, clauses_particulieres, client_id, date_creation)
VALUES
    (1, 'CTR-2026-00001', 'AUTO_TOUS_RISQUES', 'EN_VIGUEUR', '2026-03-15', '2027-03-15', 720.00, 50000.00, 300.00, 'Conducteur principal > 25 ans', 1, NOW()),
    (2, 'CTR-2026-00002', 'HABITATION_PROPRIETAIRE', 'EN_VIGUEUR', '2026-01-01', '2027-01-01', 480.00, 200000.00, 200.00, 'Alarme obligatoire', 1, NOW()),
    (3, 'CTR-2026-00003', 'AUTO_TIERS', 'EN_VIGUEUR', '2025-10-01', '2026-09-30', 350.00, 30000.00, 500.00, NULL, 2, NOW()),
    (4, 'CTR-2026-00004', 'SANTE_INDIVIDUELLE', 'RESILIE', '2025-06-01', '2026-06-01', 1200.00, 100000.00, 0.00, 'Résiliation à la demande du client', 2, NOW()),
    (5, 'CTR-2026-00005', 'MULTIRISQUE_PRO', 'EN_VIGUEUR', '2026-06-01', '2027-06-01', 3500.00, 500000.00, 1000.00, 'Couverture incendie + vol + RC', 3, NOW()),
    (6, 'CTR-2026-00006', 'AUTO_TOUS_RISQUES', 'SUSPENDU', '2026-01-01', '2026-12-31', 650.00, 45000.00, 350.00, 'Suspension pour non-paiement', 4, NOW()),
    (7, 'CTR-2026-00007', 'RC_PRO', 'EN_VIGUEUR', '2026-04-01', '2027-04-01', 2800.00, 300000.00, 500.00, NULL, 5, NOW()),
    (8, 'CTR-2026-00008', 'HABITATION_LOCATAIRE', 'EN_ATTENTE', '2026-07-15', '2027-01-15', 290.00, 80000.00, 150.00, 'En attente signature', 6, NOW()),
    (9, 'CTR-2026-00009', 'VIE', 'EN_VIGUEUR', '2026-01-01', '2051-01-01', 1500.00, 500000.00, 0.00, 'Bénéficiaire : Hélène Durand', 8, NOW()),
    (10, 'CTR-2026-00010', 'PREVOYANCE', 'EN_VIGUEUR', '2026-07-01', '2027-07-01', 840.00, 150000.00, 0.00, NULL, 9, NOW());

-- ========== TIERS ==========
INSERT INTO tiers (id, numero_tiers, nom, prenom, role, email, telephone, contrat_id)
VALUES
    (1, 'TRS-2026-00001', 'Dupont', 'Paul', 'CONDUCTEUR_SECONDAIRE', 'paul.dupont@email.fr', '+33612000001', 1),
    (2, 'TRS-2026-00002', 'Dupont', 'Lucas', 'BENEFICIAIRE', 'lucas.dupont@email.fr', '+33612000002', 2),
    (3, 'TRS-2026-00003', 'Cabinet Courtage Express', NULL, 'COURTIER', 'contact@courtage-express.fr', '+33491000001', 5),
    (4, 'TRS-2026-00004', 'Ben Ammar', 'Fatima', 'ASSURE_ADDITIONNEL', 'fatima.benammar@email.fr', '+33561000001', 6),
    (5, 'TRS-2026-00005', 'Durand', 'Helene', 'BENEFICIAIRE', 'helene.durand@email.fr', '+33240000001', 9),
    (6, 'TRS-2026-00006', 'Durand', 'Thomas', 'AYANT_DROIT', 'thomas.durand@email.fr', '+33240000002', 9);