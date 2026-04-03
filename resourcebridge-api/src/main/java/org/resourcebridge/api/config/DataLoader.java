package org.resourcebridge.api.config;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.*;
import org.resourcebridge.api.enums.*;
import static org.resourcebridge.api.enums.DonationType.*;
import org.resourcebridge.api.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final NeedRepository needRepository;
    private final DonationRepository donationRepository;
    private final AnnouncementRepository announcementRepository;
    private final TransferRepository transferRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Skip seeding if data already exists
        if (organizationRepository.count() > 0) return;

        // ── ORGANIZATIONS ──────────────────────────────────────────
        Organization nazareth    = org("House of Nazareth",            "Emergency Shelter",             "75 Albert St, Moncton, NB",          "Men, women, families",                "info@maisonnazareth.ca",       "506-858-5702");
        Organization harvest     = org("Harvest House Atlantic",        "Emergency Shelter",             "182 High St, Moncton, NB",           "Men and women",                       "info@harvesthouseatlantic.org","506-388-4357");
        Organization crossroads  = org("Crossroads for Women",          "Transition Shelter",            "Moncton, NB (confidential)",         "Women and children fleeing violence",  "adminfo@crossroadsforwomen.ca","506-853-0811");
        Organization ywca        = org("YWCA Moncton",                  "Supportive Housing",            "135 Kendra St, Moncton, NB",         "Women and children, ages 16+",        "info@ywcamoncton.com",         "506-855-4349");
        Organization johnHoward  = org("John Howard Society of SE NB",  "Transitional Housing",          "15 Flanders Court, Moncton, NB",     "Men (justice-involved)",              "info@johnhowardsenb.com",      "506-854-3499");
        Organization peterMckee  = org("Peter McKee Community Food Centre","Food Bank + Community Kitchen","66 Capitol St, Moncton, NB",        "General public, low-income",          "info@petermckeecfc.ca",        "506-383-4281");
        Organization secondMile  = org("Second Mile Food Bank",         "Food Bank",                     "243 Lewisville Rd, Moncton, NB",     "Dieppe area and part of Moncton",     "info@secondmilefoodbank.ca",   "506-857-9121");
        Organization salvation   = org("Salvation Army Community Services","Community Services",         "32 King St, Moncton, NB",            "Families and individuals in crisis",  "moncton@salvationarmy.ca",     "506-389-9901");
        Organization svdp        = org("St. Vincent de Paul Society",   "Clothing + Household Depot",    "113 Norwood Ave, Moncton, NB",       "General public in need",              "svdp.moncton@gmail.com",       "506-857-2088");
        Organization humanity    = org("The Humanity Project",          "Meals + Outreach",              "449 St George St, Moncton, NB",      "Anyone in need",                      "TheHumanityProjectNB@gmail.com","506-382-6840");

        // ── ITEMS ──────────────────────────────────────────────────
        Item soup      = item("Canned Soup",        "Chicken noodle, tomato varieties",  "cans",    true,  ItemCategory.FOOD);
        Item beans     = item("Canned Beans",        "Black, kidney, chickpea",           "cans",    true,  ItemCategory.FOOD);
        Item milk      = item("Shelf-Stable Milk",   "1L cartons, UHT",                   "cartons", true,  ItemCategory.FOOD);
        Item granola   = item("Granola Bars",        "Individually wrapped variety pack", "boxes",   true,  ItemCategory.FOOD);
        Item formula   = item("Baby Formula",        "Powdered, 0-12 months",             "cans",    true,  ItemCategory.FOOD);
        Item toothpaste= item("Toothpaste",          "Standard tube",                     "tubes",   false, ItemCategory.HYGIENE);
        Item tshirt    = item("T-Shirts",            "Mixed sizes, men and women",        "pieces",  false, ItemCategory.CLOTHING);
        Item blanket   = item("Blankets",            "Fleece or wool",                    "pieces",  false, ItemCategory.BEDDING);
        Item shampoo   = item("Shampoo",             "250ml bottle",                      "bottles", false, ItemCategory.HYGIENE);
        Item jeans     = item("Jeans",               "Mixed sizes",                       "pieces",  false, ItemCategory.CLOTHING);

        // ── DEMO USERS ──────────────────────────────────────────────
        user("Admin User",     "admin@resourcebridge.ca",  "admin1234", Role.ADMIN, nazareth);
        user("James Staff",    "staff@resourcebridge.ca",  "demo1234",  Role.STAFF, nazareth);
        user("Marie Lefebvre", "marie@harvesthouse.ca",    "demo1234",  Role.STAFF, harvest);
        user("Tom Walsh",      "tom@crossroads.ca",        "demo1234",  Role.STAFF, crossroads);

        // ── INVENTORY ──────────────────────────────────────────────
        inventory(nazareth, soup,      24, LocalDate.now().plusDays(5));   // expiring soon!
        inventory(nazareth, blanket,   12, null);
        inventory(nazareth, toothpaste,30, null);
        inventory(nazareth, milk,       8, LocalDate.now().plusDays(3));   // expiring soon!

        inventory(harvest, beans,     18, LocalDate.now().plusDays(60));
        inventory(harvest, tshirt,    20, null);
        inventory(harvest, shampoo,   15, null);

        inventory(crossroads, formula,  6, LocalDate.now().plusDays(4));   // expiring soon!
        inventory(crossroads, granola, 10, LocalDate.now().plusDays(20));

        inventory(peterMckee, soup,   50, LocalDate.now().plusDays(90));
        inventory(peterMckee, beans,  40, LocalDate.now().plusDays(120));

        // ── NEEDS ──────────────────────────────────────────────────
        need(crossroads,  formula,    12, Urgency.CRITICAL);
        need(nazareth,    blanket,    20, Urgency.CRITICAL);
        need(harvest,     soup,       30, Urgency.HIGH);
        need(ywca,        formula,     8, Urgency.HIGH);
        need(johnHoward,  tshirt,     15, Urgency.HIGH);
        need(humanity,    granola,    24, Urgency.MEDIUM);
        need(salvation,   toothpaste, 50, Urgency.MEDIUM);
        need(secondMile,  milk,       20, Urgency.MEDIUM);
        need(svdp,        jeans,      10, Urgency.LOW);
        need(nazareth,    shampoo,    20, Urgency.LOW);

        // ── DONATIONS ──────────────────────────────────────────────
        // OFFERED donations — will show as available
        Donation d1 = donation("Marie Tremblay",  "marie.t@gmail.com",     blanket,    15, null,                         DonationStatus.OFFERED,   DonationType.DROP_OFF,      null,                         null);
        Donation d2 = donation("John Smith",      "john.s@outlook.com",    soup,       24, LocalDate.now().plusDays(60), DonationStatus.OFFERED,   DonationType.PICKUP_REQUEST,"45 Elmwood Dr, Moncton",     "Moncton");
        Donation d3 = donation("Isabelle Roy",    "isabelle@hotmail.com",  formula,     6, LocalDate.now().plusDays(30), DonationStatus.OFFERED,   DonationType.PICKUP_REQUEST,"112 Champlain St, Dieppe",   "Dieppe");
        // Already-processed donations for demo history
        Donation d4 = donation("Robert Chen",     "rchen@gmail.com",       tshirt,     20, null,                         DonationStatus.ASSIGNED,  DonationType.DROP_OFF,      null,                         null);
        Donation d5 = donation("Nadia Williams",  "nadia.w@gmail.com",     granola,    16, LocalDate.now().plusDays(45), DonationStatus.DELIVERED, DonationType.PICKUP_REQUEST,"88 Pine St, Riverview",      "Riverview");
        Donation d6 = donation("François Leblanc","fleblanc@gmail.com",    toothpaste, 35, null,                         DonationStatus.RECEIVED,  DonationType.DROP_OFF,      null,                         null);

        // ── ANNOUNCEMENTS ──────────────────────────────────────────
        announcement(nazareth,   milk,    8,  AnnouncementType.EXPIRY,  "Shelf-stable milk expiring in 3 days — can any org use these?");
        announcement(crossroads, formula, 6,  AnnouncementType.URGENT,  "Critically low on baby formula — urgent need for 12+ cans");
        announcement(peterMckee, soup,   20,  AnnouncementType.SURPLUS, "Surplus canned soup available — first come first served");
        announcement(harvest,    tshirt, 15,  AnnouncementType.SURPLUS, "Clothing surplus: T-shirts in all sizes available for pickup");
        announcement(nazareth,   soup,    8,  AnnouncementType.EXPIRY,  "Canned soup expiring soon — redistributing to shelters in need");

        // ── TRANSFERS (auto-matched history for demo) ───────────────
        transfer(d4, johnHoward, 15, TransferStatus.IN_TRANSIT);
        transfer(d5, humanity,   16, TransferStatus.COMPLETED);
        transfer(d6, salvation,  35, TransferStatus.COMPLETED);

        System.out.println("✅ ResourceBridge seed data loaded successfully!");
    }

    // ── HELPERS ────────────────────────────────────────────────────

    private Organization org(String name, String type, String address, String pop, String email, String phone) {
        Organization o = new Organization();
        o.setName(name); o.setType(type); o.setAddress(address);
        o.setPopulationServed(pop); o.setContactEmail(email); o.setContactPhone(phone);
        return organizationRepository.save(o);
    }

    private Item item(String name, String desc, String unit, boolean expiryRelevant, ItemCategory category) {
        Item i = new Item();
        i.setName(name); i.setDescription(desc); i.setUnit(unit);
        i.setExpiryRelevant(expiryRelevant); i.setCategory(category);
        return itemRepository.save(i);
    }

    private void user(String name, String email, String password, Role role, Organization org) {
        User u = new User();
        u.setName(name); u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(role); u.setOrganization(org);
        userRepository.save(u);
    }

    private void inventory(Organization org, Item item, int qty, LocalDate expiry) {
        Inventory inv = new Inventory();
        inv.setOrganization(org); inv.setItem(item);
        inv.setQuantity(qty); inv.setExpiryDate(expiry);
        inventoryRepository.save(inv);
    }

    private void need(Organization org, Item item, int qty, Urgency urgency) {
        Need n = new Need();
        n.setOrganization(org); n.setItem(item);
        n.setQuantityNeeded(qty); n.setUrgency(urgency);
        n.setFulfilled(false); n.setCreatedAt(LocalDateTime.now());
        needRepository.save(n);
    }

    private Donation donation(String donorName, String donorEmail, Item item, int qty, LocalDate expiry,
                              DonationStatus status, DonationType donationType,
                              String pickupAddress, String pickupCity) {
        Donation d = new Donation();
        d.setDonorName(donorName); d.setDonorEmail(donorEmail);
        d.setItem(item); d.setQuantity(qty);
        d.setExpiryDate(expiry); d.setStatus(status);
        d.setDonationType(donationType);
        d.setPickupAddress(pickupAddress);
        d.setPickupCity(pickupCity);
        d.setCreatedAt(LocalDateTime.now());
        return donationRepository.save(d);
    }

    private void announcement(Organization org, Item item, int qty, AnnouncementType type, String message) {
        Announcement a = new Announcement();
        a.setOrganization(org); a.setItem(item);
        a.setQuantity(qty); a.setType(type);
        a.setMessage(message); a.setCreatedAt(LocalDateTime.now());
        announcementRepository.save(a);
    }

    private void transfer(Donation donation, Organization toOrg, int qty, TransferStatus status) {
        Transfer t = new Transfer();
        t.setDonation(donation); t.setToOrganization(toOrg);
        t.setQuantityAssigned(qty);
        t.setStatus(status); t.setCreatedAt(LocalDateTime.now());
        transferRepository.save(t);
    }
}
