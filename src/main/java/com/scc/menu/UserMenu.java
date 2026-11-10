package com.scc.menu;

import com.scc.dao.BorrowingDao;
import com.scc.dao.EquipmentDao;
import com.scc.dao.EventDao;
import com.scc.dao.UserDao;
import com.scc.model.BorrowingTransaction;
import com.scc.model.Equipment;
import com.scc.model.Event;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

import static com.scc.menu.MenuUtil.*;

public class UserMenu {

    private final UserDao userDao;
    private final EquipmentDao equipmentDao;
    private final EventDao eventDao;
    private final BorrowingDao borrowingDao;
    private final Scanner sc;
    private final User user;

    public UserMenu(UserDao userDao, Scanner sc, User user) {
        this.userDao = userDao;
        this.sc = sc;
        this.user = user;
        this.equipmentDao = new EquipmentDao();
        this.eventDao = new EventDao();
        this.borrowingDao = new BorrowingDao();
    }

    public void start() {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("             USER DASHBOARD");
            System.out.println("========================================");
            System.out.println("Welcome, " + user.username());
            System.out.println("1. View & Search Available Equipment");
            System.out.println("2. Create Event Record");
            System.out.println("3. View My Events");
            System.out.println("4. Submit Equipment Borrowing Request");
            System.out.println("5. View My Borrowing History & Status");
            System.out.println("6. View / Update My Profile");
            System.out.println("7. Logout");
            System.out.println("========================================");

            switch (readLine(sc, "Enter your choice: ")) {
                case "1" -> displayEquipment(equipmentDao.getAllEquipment());
                case "2" -> createEvent();
                case "3" -> displayMyEvents();
                case "4" -> submitBorrowingRequest();
                case "5" -> displayMyBorrowingHistory();
                case "6" -> profile();
                case "7" -> { System.out.println("Logging out..."); return; }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void displayEquipment(List<Equipment> list) {
        System.out.println("\n================ AVAILABLE EQUIPMENT ================");
        if (list.isEmpty()) {
            System.out.println("No equipment available.");
            return;
        }
        System.out.printf("%-5s | %-18s | %-12s | %-8s | %-10s%n", "ID", "Name", "Category", "Available", "Condition");
        line(66);
        for (Equipment eq : list) {
            System.out.printf("%-5d | %-18s | %-12s | %-8d | %-10s%n",
                    eq.equipmentId(), shorten(eq.name(), 18), shorten(eq.category(), 12),
                    eq.availableQuantity(), eq.condition());
        }
    }

    private void createEvent() {
        System.out.println("\n========== CREATE EVENT RECORD ==========");
        String name = readLine(sc, "Event name: ");
        String date = readLine(sc, "Event date (YYYY-MM-DD): ");
        String location = readLine(sc, "Location: ");
        int guests = readInt(sc, "Number of guests: ");
        System.out.println(eventDao.createEvent(user.userId(), name, date, location, guests)
                ? "Event record created." : "Failed to create event record.");
    }

    private void displayMyEvents() {
        List<Event> events = eventDao.getEventsByUserId(user.userId());
        System.out.println("\n================ MY EVENTS ================");
        if (events.isEmpty()) {
            System.out.println("No events found. Create an event first.");
            return;
        }
        System.out.printf("%-5s | %-25s | %-12s | %-15s | %-8s%n", "ID", "Event Name", "Date", "Location", "Guests");
        line(76);
        for (Event e : events) {
            System.out.printf("%-5d | %-25s | %-12s | %-15s | %-8d%n",
                    e.eventId(), shorten(e.name(), 25), e.eventDate(), shorten(e.location(), 15), e.guests());
        }
    }

    private void submitBorrowingRequest() {
        System.out.println("\n========== SUBMIT BORROWING REQUEST ==========");
        if (eventDao.getEventsByUserId(user.userId()).isEmpty()) {
            System.out.println("You must create an event record before borrowing equipment.");
            return;
        }
        displayMyEvents();
        int eventId = readInt(sc, "Event ID for this request: ");
        displayEquipment(equipmentDao.getAllEquipment());
        int equipmentId = readInt(sc, "Equipment ID to borrow: ");
        int qty = readInt(sc, "Quantity: ");
        String dueDate = readLine(sc, "Return due date (YYYY-MM-DD): ");
        System.out.println(borrowingDao.createBorrowingRequest(eventId, user.userId(), equipmentId, qty, dueDate)
                ? "Borrowing request submitted. Status: PENDING." : "Failed to submit borrowing request.");
    }

    private void displayMyBorrowingHistory() {
        List<BorrowingTransaction> list = borrowingDao.getTransactionsByUserId(user.userId());
        System.out.println("\n================ MY BORROWING HISTORY ================");
        if (list.isEmpty()) {
            System.out.println("No borrowing transactions found.");
            return;
        }
        System.out.printf("%-5s | %-8s | %-8s | %-6s | %-12s | %-10s | %-12s%n",
                "ID", "Event ID", "Equip ID", "Qty", "Due Date", "Status", "Return");
        line(80);
        for (BorrowingTransaction t : list) {
            System.out.printf("%-5d | %-8d | %-8d | %-6d | %-12s | %-10s | %-12s%n",
                    t.borrowingId(), t.eventId(), t.equipmentId(), t.quantity(), t.dueDate(),
                    t.requestStatus(), t.returnDate() == null ? "-" : t.returnDate());
        }
    }

    private void profile() {
        System.out.println("\n========================================");
        System.out.println("              MY PROFILE");
        System.out.println("========================================");
        System.out.println("User ID  : " + user.userId());
        System.out.println("Username : " + user.username());
        System.out.println("Email    : " + user.email());
        System.out.println("Role     : " + user.role());
        System.out.println("Status   : " + user.status());
        System.out.println("========================================");
        if (readLine(sc, "Update your email? (y/n): ").equalsIgnoreCase("y")) {
            String email = readLine(sc, "New email: ");
            System.out.println(userDao.updateProfile(user.userId(), email)
                    ? "Profile updated." : "Update failed.");
        }
    }
}
