package com.example.Stage.Config;

import com.example.Stage.Model.*;
import com.example.Stage.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final PolicyRepository policyRepository;
    private final PolicyRoleRepository policyRoleRepository;
    private final CoverageRepository coverageRepository;
    private final BillRepository billRepository;
    private final EndorsementRepository endorsementRepository;

    private final Random random = new Random(42);

    public DataLoader(ProductRepository productRepository,
                      ThirdPartyRepository thirdPartyRepository,
                      PolicyRepository policyRepository,
                      PolicyRoleRepository policyRoleRepository,
                      CoverageRepository coverageRepository,
                      BillRepository billRepository,
                      EndorsementRepository endorsementRepository) {
        this.productRepository = productRepository;
        this.thirdPartyRepository = thirdPartyRepository;
        this.policyRepository = policyRepository;
        this.policyRoleRepository = policyRoleRepository;
        this.coverageRepository = coverageRepository;
        this.billRepository = billRepository;
        this.endorsementRepository = endorsementRepository;
    }

    @Override
    public void run(String... args) {
        if (policyRepository.count() > 0) return; // Already loaded

        System.out.println("=== Generating test data... ===");

        List<Product> products = createProducts();
        List<ThirdParty> thirdParties = createThirdParties();
        List<Policy> policies = createPolicies(products, thirdParties);
        createRoles(policies, thirdParties);
        createCoverages(policies);
        createBills(policies);
        createEndorsements(policies);

        System.out.println("=== Data generation complete: " + policies.size() + " policies ===");
    }

    // ========== PRODUCTS ==========
    private List<Product> createProducts() {
        String[][] data = {
                {"EPARGNE_VIE", "Epargne Vie Plus", "Contrat epargne en euros", "SAVINGS"},
                {"MULTI_SUPPORT", "Multi Support Dynamique", "Contrat multi-supports UC", "UNIT_LINKED"},
                {"TEMPO_DECES", "Temporaire Deces", "Assurance temporaire deces", "TERM_LIFE"},
                {"PER_INDIVIDUEL", "PER Individuel", "Plan Epargne Retraite", "RETIREMENT"},
                {"CAPITALISATION", "Capitalisation Pro", "Contrat de capitalisation", "CAPITALIZATION"},
                {"VIE_ENTIERE", "Vie Entiere", "Assurance vie entiere", "WHOLE_LIFE"},
                {"PREVOYANCE", "Prevoyance Famille", "Contrat de prevoyance", "PROTECTION"},
                {"RETRAITE_COL", "Retraite Collective", "Contrat retraite entreprise", "GROUP_RETIREMENT"},
        };

        List<Product> products = new ArrayList<>();
        for (String[] d : data) {
            Product p = new Product();
            p.setProductCode(d[0]);
            p.setName(d[1]);
            p.setDescription(d[2]);
            p.setType(d[3]);
            p.setVersion("1.0");
            products.add(productRepository.save(p));
        }
        return products;
    }

    // ========== THIRD PARTIES ==========
    private List<ThirdParty> createThirdParties() {
        String[][] persons = {
                {"Dupont", "Marie", "1975-03-15", "FEMALE", "Paris", "75001"},
                {"Martin", "Jean", "1982-07-22", "MALE", "Lyon", "69001"},
                {"Durand", "Pierre", "1965-01-12", "MALE", "Nantes", "44000"},
                {"Moreau", "Claire", "1978-09-25", "FEMALE", "Strasbourg", "67000"},
                {"Petit", "Sophie", "1988-05-30", "FEMALE", "Bordeaux", "33000"},
                {"Ben Ammar", "Karim", "1990-11-08", "MALE", "Toulouse", "31000"},
                {"Leroy", "Thomas", "1985-02-14", "MALE", "Lille", "59000"},
                {"Roux", "Isabelle", "1970-08-19", "FEMALE", "Marseille", "13001"},
                {"Simon", "Antoine", "1992-12-03", "MALE", "Nice", "06000"},
                {"Laurent", "Emilie", "1983-04-27", "FEMALE", "Montpellier", "34000"},
                {"Garcia", "Carlos", "1979-06-11", "MALE", "Perpignan", "66000"},
                {"Bernard", "Nathalie", "1987-10-08", "FEMALE", "Rennes", "35000"},
                {"Dubois", "Philippe", "1968-03-22", "MALE", "Dijon", "21000"},
                {"Thomas", "Valerie", "1991-07-16", "FEMALE", "Tours", "37000"},
                {"Robert", "Michel", "1963-11-29", "MALE", "Grenoble", "38000"},
                {"Richard", "Anne", "1976-01-05", "FEMALE", "Rouen", "76000"},
                {"Blanc", "Julien", "1994-09-18", "MALE", "Metz", "57000"},
                {"Girard", "Catherine", "1972-05-24", "FEMALE", "Reims", "51100"},
                {"Faure", "Olivier", "1986-08-07", "MALE", "Clermont-Ferrand", "63000"},
                {"Mercier", "Stephanie", "1981-02-13", "FEMALE", "Angers", "49000"},
                {"Dupont", "Paul", "1972-06-18", "MALE", "Paris", "75001"},
                {"Durand", "Helene", "1968-04-03", "FEMALE", "Nantes", "44000"},
                {"Martin", "Lucie", "2005-03-10", "FEMALE", "Lyon", "69001"},
                {"Moreau", "Alexandre", "1995-11-20", "MALE", "Strasbourg", "67000"},
                {"Leroy", "Christine", "1960-07-30", "FEMALE", "Lille", "59000"},
        };

        String[][] companies = {
                {"SCI Les Oliviers", "Marseille", "13008", "44512345600021"},
                {"Cabinet Moreau et Fils", "Paris", "75004", "52398765400015"},
                {"Garage Leroy SARL", "Lille", "59000", "33256789000042"},
                {"Restaurant Chez Paul", "Nice", "06000", "41587654300028"},
                {"Pharmacie Central", "Lyon", "69002", "55123456700033"},
                {"SCI Residence du Parc", "Bordeaux", "33000", "66234567800044"},
                {"Transport Express SARL", "Toulouse", "31000", "77345678900055"},
                {"Boulangerie Artisan", "Rennes", "35000", "88456789000066"},
                {"Cabinet Conseil RH", "Paris", "75008", "99567890100077"},
                {"Informatique Pro SAS", "Montpellier", "34000", "11678901200088"},
        };

        List<ThirdParty> all = new ArrayList<>();

        int idCounter = 1;
        for (String[] p : persons) {
            ThirdParty tp = new ThirdParty();
            tp.setIdentifier(String.format("TP-%03d", idCounter++));
            tp.setType("PHYSICAL_PERSON");
            tp.setName(p[0]);
            tp.setFirstName(p[1]);
            tp.setBirthDate(LocalDate.parse(p[2]));
            tp.setGender(p[3]);
            tp.setTown(p[4]);
            tp.setPostalCode(p[5]);
            tp.setCountry("FR");
            tp.setEmail(p[1].toLowerCase() + "." + p[0].toLowerCase().replace(" ", "") + "@email.fr");
            tp.setPhone("+336" + String.format("%08d", random.nextInt(100000000)));
            tp.setStreet((random.nextInt(50) + 1) + " rue de la Paix");
            all.add(thirdPartyRepository.save(tp));
        }

        for (String[] c : companies) {
            ThirdParty tp = new ThirdParty();
            tp.setIdentifier(String.format("TP-%03d", idCounter++));
            tp.setType("LEGAL_ENTITY");
            tp.setName(c[0]);
            tp.setTown(c[1]);
            tp.setPostalCode(c[2]);
            tp.setCountry("FR");
            tp.setNationalIdentifier(c[3]);
            tp.setNationalIdentifierCountry("FR");
            tp.setEmail("contact@" + c[0].toLowerCase().replace(" ", "-") + ".fr");
            tp.setPhone("+334" + String.format("%08d", random.nextInt(100000000)));
            tp.setStreet((random.nextInt(50) + 1) + " avenue des Entreprises");
            all.add(thirdPartyRepository.save(tp));
        }

        return all;
    }

    // ========== POLICIES ==========
    private List<Policy> createPolicies(List<Product> products, List<ThirdParty> thirdParties) {
        String[] statuses = {"ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE",
                "CANCELLED", "SUSPENDED", "PENDING"};
        String[] periodicities = {"MONTHLY", "QUARTERLY", "YEARLY"};
        String[] paymentModes = {"DIRECT_DEBIT", "TRANSFER"};
        String[] premiumTypes = {"REGULAR", "SINGLE"};

        List<Policy> policies = new ArrayList<>();

        for (int i = 1; i <= 220; i++) {
            // Policy number format: XXX-YYY
            int group = ((i - 1) / 10) + 1;
            int sub = ((i - 1) % 10) + 1;
            String policyNumber = String.format("%03d-%03d", group, sub);

            Policy p = new Policy();
            p.setPolicyNumber(policyNumber);
            p.setStatus(statuses[random.nextInt(statuses.length)]);
            p.setProduct(products.get(random.nextInt(products.size())));
            p.setHolder(thirdParties.get(random.nextInt(thirdParties.size())));

            int yearOffset = random.nextInt(8);
            p.setEffectiveDate(LocalDate.of(2019 + yearOffset, random.nextInt(12) + 1, random.nextInt(28) + 1));
            p.setTermDate(p.getEffectiveDate().plusYears(10 + random.nextInt(20)));
            p.setCurrency("EUR");
            p.setPaymentPeriodicity(periodicities[random.nextInt(periodicities.length)]);
            p.setPremiumType(premiumTypes[random.nextInt(premiumTypes.length)]);
            p.setPaymentMode(paymentModes[random.nextInt(paymentModes.length)]);

            BigDecimal premium = BigDecimal.valueOf(200 + random.nextInt(4800));
            p.setAnnualPremium(premium);

            if (!"PENDING".equals(p.getStatus())) {
                p.setSurrenderValue(BigDecimal.valueOf(premium.intValue() * (2 + random.nextInt(20))));
            }

            p.setTaxCountry("FR");
            policies.add(policyRepository.save(p));
        }

        return policies;
    }

    // ========== ROLES ==========
    private void createRoles(List<Policy> policies, List<ThirdParty> thirdParties) {
        String[] extraRoles = {"BENEFICIARY", "LIFE_ASSURED", "PAYER", "BROKER"};

        for (Policy policy : policies) {
            // Holder role
            PolicyRole holderRole = new PolicyRole();
            holderRole.setPolicy(policy);
            holderRole.setThirdParty(policy.getHolder());
            holderRole.setRoleType("HOLDER");
            policyRoleRepository.save(holderRole);

            // 50% chance of additional role
            if (random.nextBoolean()) {
                PolicyRole extraRole = new PolicyRole();
                extraRole.setPolicy(policy);
                extraRole.setThirdParty(thirdParties.get(random.nextInt(thirdParties.size())));
                extraRole.setRoleType(extraRoles[random.nextInt(extraRoles.length)]);
                policyRoleRepository.save(extraRole);
            }

            // 20% chance of a second extra role
            if (random.nextInt(5) == 0) {
                PolicyRole extraRole2 = new PolicyRole();
                extraRole2.setPolicy(policy);
                extraRole2.setThirdParty(thirdParties.get(random.nextInt(thirdParties.size())));
                extraRole2.setRoleType(extraRoles[random.nextInt(extraRoles.length)]);
                policyRoleRepository.save(extraRole2);
            }
        }
    }

    // ========== COVERAGES ==========
    private void createCoverages(List<Policy> policies) {
        String[][] coverageTypes = {
                {"Garantie Deces", "DEATH"},
                {"Garantie Epargne", "SAVINGS"},
                {"Garantie UC", "UNIT_LINKED"},
                {"Garantie Invalidite", "DISABILITY"},
                {"Garantie Capitalisation", "CAPITALIZATION"},
                {"Garantie Retraite", "RETIREMENT"},
        };

        int covId = 1;
        for (Policy policy : policies) {
            // Main coverage
            String[] mainCov = coverageTypes[random.nextInt(coverageTypes.length)];
            Coverage c = new Coverage();
            c.setIdentifier(String.format("COV-%04d", covId++));
            c.setLabel(mainCov[0]);
            c.setCoverageType(mainCov[1]);
            c.setStatus("CANCELLED".equals(policy.getStatus()) ? "CANCELLED" : "ACTIVE");
            c.setCapital(BigDecimal.valueOf(50000 + random.nextInt(450000)));
            c.setCapitalCurrency("EUR");
            c.setPremium(policy.getAnnualPremium().multiply(BigDecimal.valueOf(0.6)));
            c.setPremiumCurrency("EUR");
            c.setBeginDate(policy.getEffectiveDate());
            c.setEndDate(policy.getTermDate());
            c.setMainCover(true);
            c.setPolicy(policy);
            coverageRepository.save(c);

            // 40% chance of second coverage
            if (random.nextInt(5) < 2) {
                String[] secCov = coverageTypes[random.nextInt(coverageTypes.length)];
                Coverage c2 = new Coverage();
                c2.setIdentifier(String.format("COV-%04d", covId++));
                c2.setLabel(secCov[0]);
                c2.setCoverageType(secCov[1]);
                c2.setStatus(c.getStatus());
                c2.setCapital(BigDecimal.valueOf(20000 + random.nextInt(100000)));
                c2.setCapitalCurrency("EUR");
                c2.setPremium(policy.getAnnualPremium().multiply(BigDecimal.valueOf(0.4)));
                c2.setPremiumCurrency("EUR");
                c2.setBeginDate(policy.getEffectiveDate());
                c2.setEndDate(policy.getTermDate());
                c2.setMainCover(false);
                c2.setPolicy(policy);
                coverageRepository.save(c2);
            }
        }
    }

    // ========== BILLS ==========
    private void createBills(List<Policy> policies) {
        String[] billStatuses = {"PAID", "PAID", "PAID", "PENDING", "UNPAID"};

        int billId = 1;
        for (Policy policy : policies) {
            if ("PENDING".equals(policy.getStatus())) continue;

            int numBills = 1 + random.nextInt(4);
            for (int b = 0; b < numBills; b++) {
                Bill bill = new Bill();
                bill.setIdentifier(String.format("BILL-%04d", billId++));
                bill.setEffectiveDate(policy.getEffectiveDate().plusMonths(b * 3L));
                bill.setIssuingDate(bill.getEffectiveDate().minusDays(15));
                bill.setEndDate(bill.getEffectiveDate().plusMonths(3));
                bill.setCurrency("EUR");

                String status = billStatuses[random.nextInt(billStatuses.length)];
                bill.setStatus(status);

                BigDecimal amount;
                if ("YEARLY".equals(policy.getPaymentPeriodicity())) {
                    amount = policy.getAnnualPremium();
                } else if ("QUARTERLY".equals(policy.getPaymentPeriodicity())) {
                    amount = policy.getAnnualPremium().divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
                } else {
                    amount = policy.getAnnualPremium().divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
                }

                bill.setBillAmount(amount);
                bill.setPaidAmount("PAID".equals(status) ? amount : BigDecimal.ZERO);
                bill.setReminderStatus("UNPAID".equals(status) ? "REMINDER_1" : null);
                bill.setPolicy(policy);
                billRepository.save(bill);
            }
        }
    }

    // ========== ENDORSEMENTS ==========
    private void createEndorsements(List<Policy> policies) {
        String[] types = {"ADDITION", "MODIFICATION", "WITHDRAWAL"};
        String[] subTypes = {"ADD_COVERAGE", "CHANGE_BENEFICIARY", "CHANGE_PAYMENT_MODE", "PARTIAL_SURRENDER", "TOTAL_SURRENDER"};
        String[] statuses = {"VALIDATED", "VALIDATED", "PENDING", "REJECTED"};

        int endId = 1;
        for (Policy policy : policies) {
            // 30% chance of having an endorsement
            if (random.nextInt(10) < 3) {
                Endorsement e = new Endorsement();
                e.setIdentifier(String.format("END-%04d", endId++));
                e.setEffectiveDate(policy.getEffectiveDate().plusMonths(6 + random.nextInt(24)));
                e.setStatus(statuses[random.nextInt(statuses.length)]);
                e.setEndorsementType(types[random.nextInt(types.length)]);
                e.setEndorsementSubType(subTypes[random.nextInt(subTypes.length)]);

                if (random.nextBoolean()) {
                    e.setGrossAmount(BigDecimal.valueOf(500 + random.nextInt(9500)));
                    e.setGrossAmountCurrency("EUR");
                }

                e.setPolicy(policy);
                endorsementRepository.save(e);
            }
        }
    }
}