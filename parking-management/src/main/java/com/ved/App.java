package com.ved;

import com.ved.dao.ParkingManagementDAO;
import com.ved.model.Vehicle;
import com.ved.model.ParkingRecord;
import com.ved.model.ParkingSlot;

import java.util.Scanner;
import java.util.List;
import java.time.format.DateTimeFormatter;
//import java.util.List;

public class App {
    private static final ParkingManagementDAO dao = new ParkingManagementDAO();
    private static final Scanner scanner = new Scanner(System.in);
    public static void main(String[] args) {
        while (true) {
            System.out.println("\nParking Management System");
            System.out.println("1. Add/Register New Vehicle");
            System.out.println("2. Park Vehicle");
            System.out.println("3. View Parked Vehicles");
            System.out.println("4. Remove Parked Vehicle");
            System.out.println("5. Add Parking Slots");
            System.out.println("6. View Available Slots");
            System.out.println("7. Update Parking Slot");
            System.out.println("8. Show All Registered Vehicles");
            System.out.println("9. Show All Parking Records");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            switch (choice) {
                case 1:
                    addNewVehicle();
                    break;
                case 2:
                    parkVehicle();
                    break;
                case 3:
                    showParkedVehicles();
                    break;
                case 4:
                    removeVehicle();
                    break;
                case 5:
                    addParkingSlots();
                    break;
                case 6:
                    viewAvailableSlots();
                    break;
                case 7:
                    updateParkingSlot();
                    break;
                case 8:
                    showAllVehicles();
                    break;
                case 9:
                	showAllParkingRecords();
                	break;
                case 0:
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void addNewVehicle() {
        Vehicle vehicle = new Vehicle();
        System.out.print("Enter your name: ");
        vehicle.setVehicleDriverName(scanner.nextLine());
        System.out.print("Enter license plate: ");
        vehicle.setLicensePlate(scanner.nextLine());
        System.out.print("Enter vehicle type (CAR/BIKE): ");
        vehicle.setVehicleType(scanner.nextLine());
        dao.saveVehicle(vehicle);
        System.out.println("Vehicle added successfully!");
    }
    
    private static void addParkingSlots() {
        System.out.println("\n=== Add Parking Slots ===");

        // Get current slot count
        int currentSlots = dao.getTotalParkingSlots();
        System.out.println("Current total parking slots: " + currentSlots);

        System.out.print("Enter slot type (CAR/BIKE): ");
        String slotType = scanner.nextLine().toUpperCase();

        // Validate slot type
        if (!slotType.equals("CAR") && !slotType.equals("BIKE")) {
            System.out.println("Invalid slot type! Must be CAR or BIKE");
            return;
        }

        System.out.print("Enter the number of slots to add: ");
        int numberOfSlots = scanner.nextInt();
        scanner.nextLine();

//        System.out.print("Enter the capacity for each slot: ");
//        int capacity = scanner.nextInt();
//        scanner.nextLine();
        
        int capacity =1;
        if (numberOfSlots <= 0 || capacity <= 0) {
            System.out.println("Invalid input! Both number of slots and capacity must be positive numbers.");
            return;
        }

        // Add multiple slots
        for (int i = 1; i <= numberOfSlots; i++) {
            ParkingSlot slot = new ParkingSlot();
            String slotNumber = "PS" + (currentSlots + i);
            slot.setSlotNumber(slotNumber);
            slot.setSlotType(slotType);
            slot.setOccupied(false);
            slot.setCapacity(capacity);

            dao.saveParkingSlot(slot);

            System.out.println("Added Slot: Number = " + slotNumber + ", Type = " + slotType + ", Capacity = " + capacity);
            //System.out.println("Added Slot: Number = " + slotNumber + ", Type = " + slotType);
        }

        System.out.println("Successfully added " + numberOfSlots + " slots!");
    }


    
    
    

    private static void parkVehicle() {
        System.out.println("\n=== Park Vehicle ===");
        
        // Get vehicle details
        System.out.print("Enter vehicle license plate: ");
        String licensePlate = scanner.nextLine();
        
        // Find available slots
        List<ParkingSlot> availableSlots = dao.getAvailableSlots();
        if (availableSlots.isEmpty()) {
            System.out.println("Sorry, no parking slots are available!");
            return;
        }
        
        // Display available slots
        System.out.println("\nAvailable Slots:");
        for (ParkingSlot slot : availableSlots) {
            System.out.println(slot.getId() + ". Slot Number: " + slot.getSlotNumber() + 
                             ", Type: " + slot.getSlotType());
        }
        
        // Get slot choice
        System.out.print("\nEnter slot ID to park in: ");
        Long slotId = scanner.nextLong();
        scanner.nextLine(); // Consume newline
        
        // Create parking record
        try {
            Vehicle vehicle = dao.findVehicleByLicensePlate(licensePlate);
            ParkingSlot slot = dao.findParkingSlotById(slotId);
            
            if (vehicle == null) {
                System.out.println("Vehicle not found! Please register the vehicle first.");
                return;
            }
            
            if (slot == null) {
                System.out.println("Invalid slot ID!");
                return;
            }
            
            if (slot.isOccupied()) {
                System.out.println("This slot is already occupied!");
                return;
            }
            
            // Check if vehicle type matches slot type
            if (!vehicle.getVehicleType().equalsIgnoreCase(slot.getSlotType())) {
                System.out.println("Vehicle type does not match slot type!");
                return;
            }
            
            ParkingRecord record = dao.createParkingRecord(vehicle, slot);
            System.out.println("Vehicle parked successfully!");
            System.out.println("Entry Time: " + record.getEntryTime());
            System.out.println("Slot Number: " + slot.getSlotNumber());
            
        } catch (Exception e) {
            System.out.println("Error parking vehicle: " + e.getMessage());
        }
    }

    private static void removeVehicle() {
        System.out.println("\n=== Remove Vehicle ===");
        
        // Get vehicle details
        System.out.print("Enter vehicle license plate: ");
        String licensePlate = scanner.nextLine();
        
        try {
            // Find active parking record
            ParkingRecord record = dao.findActiveParkingRecord(licensePlate);
            
            if (record == null) {
                System.out.println("No active parking record found for this vehicle!");
                return;
            }
            
            // Complete parking record
            dao.completeParkingRecord(record);
            
            // Display parking details
            System.out.println("\nParking Details:");
            System.out.println("Entry Time: " + record.getEntryTime());
            System.out.println("Exit Time: " + record.getExitTime());
            System.out.println("Parking Fee: ₹" + record.getParkingFee());
            
        } catch (Exception e) {
            System.out.println("Error removing vehicle: " + e.getMessage());
        }
    }

    private static void viewAvailableSlots() {
        List<ParkingSlot> availableSlots = dao.getAvailableSlots();
        if (availableSlots.isEmpty()) {
            System.out.println("No available slots!");
            return;
        }
        System.out.println("Available Slots:");
        for (ParkingSlot slot : availableSlots) {
            System.out.println("Slot Number: " + slot.getSlotNumber() + 
                             ", Type: " + slot.getSlotType());
        }
    }
    
    private static void showParkedVehicles() {
        System.out.println("\n=== Currently Parked Vehicles ===");
        
        List<ParkingRecord> activeRecords = dao.getActiveParkingRecords();
        
        if (activeRecords.isEmpty()) {
            System.out.println("No vehicles are currently parked.");
            return;
        }
        
        System.out.println("\nParked Vehicles List:");
        System.out.println("------------------------------------------------------");
        System.out.printf("%-15s %-12s %-12s %-20s%n", 
            "License Plate", "Vehicle Type", "Slot Number", "Entry Time");
        System.out.println("------------------------------------------------------");
        
        for (ParkingRecord record : activeRecords) {
            Vehicle vehicle = record.getVehicle();
            ParkingSlot slot = record.getParkingSlot();
            String entryTime = record.getEntryTime().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );
            System.out.printf("%-15s %-12s %-12s %-20s%n",
                vehicle.getLicensePlate(),
                vehicle.getVehicleType(),
                slot.getSlotNumber(),
                entryTime
            );
        }
        System.out.println("------------------------------------------------------");
        
        // Show total count
        System.out.println("\nTotal Vehicles Parked: " + activeRecords.size());
    }
    
    
    private static void updateParkingSlot() {
        System.out.println("\n=== Update Parking Slot ===");
        
        // Show all slots first
        List<ParkingSlot> allSlots = dao.getAllParkingSlots();
        System.out.println("\nAll Parking Slots:");
        System.out.println("------------------------------------------------------");
        System.out.printf("%-5s %-12s %-12s %-10s%n", "ID", "Slot Number", "Type", "Status");
        System.out.println("------------------------------------------------------");
        
        for (ParkingSlot slot : allSlots) {
            System.out.printf("%-5d %-12s %-12s %-10s%n",
                slot.getId(),
                slot.getSlotNumber(),
                slot.getSlotType(),
                slot.isOccupied() ? "Occupied" : "Available");
        }
        
        System.out.print("\nEnter slot ID to update: ");
        Long slotId = scanner.nextLong();
        scanner.nextLine(); // Consume newline
        
        ParkingSlot slot = dao.findParkingSlotById(slotId);
        if (slot == null) {
            System.out.println("Slot not found!");
            return;
        }
        
        if (slot.isOccupied()) {
            System.out.println("Cannot update occupied slot!");
            return;
        }
        
        System.out.println("Current slot details:");
        System.out.println("Slot Number: " + slot.getSlotNumber());
        System.out.println("Type: " + slot.getSlotType());
        
        System.out.print("Enter new slot type (CAR/BIKE) or press Enter to skip: ");
        String newType = scanner.nextLine().toUpperCase();
        if (!newType.isEmpty()) {
            if (!newType.equals("CAR") && !newType.equals("BIKE")) {
                System.out.println("Invalid slot type! Must be CAR or BIKE");
                return;
            }
            slot.setSlotType(newType);
        }
        
        System.out.print("Enter new slot number or press Enter to skip: ");
        String newNumber = scanner.nextLine();
        if (!newNumber.isEmpty()) {
            slot.setSlotNumber(newNumber);
        }
        
        dao.updateParkingSlot(slot);
        System.out.println("Parking slot updated successfully!");
    }
    
    private static void showAllVehicles() {
        System.out.println("\n=== All Registered Vehicles ===");
        
        List<Vehicle> vehicles = dao.getAllVehicles();
        
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles registered in the system.");
            return;
        }
        
        System.out.println("\nRegistered Vehicles List:");
        System.out.println("------------------------------------------------------");
        System.out.printf("%-5s %-15s %-12s %-15s%n", 
            "ID", "License Plate", "Type", "Status");
        System.out.println("------------------------------------------------------");
        
        for (Vehicle vehicle : vehicles) {
            String status = dao.isVehicleParked(vehicle.getId()) ? "Parked" : "Not Parked";
            
            System.out.printf("%-5d %-15s %-12s %-15s%n",
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getVehicleType(),
                status);
        }
        System.out.println("------------------------------------------------------");
        System.out.println("\nTotal Registered Vehicles: " + vehicles.size());
    }
    
    private static void showAllParkingRecords() {
        System.out.println("\n=== All Parking Records ===");

        // Fetch all parking records (active and completed)
        List<ParkingRecord> parkingRecords = dao.getActiveParkingRecords();

        if (parkingRecords.isEmpty()) {
            System.out.println("No parking records found!");
            return;
        }

        // Display table header
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.printf("%-15s %-12s %-12s %-20s %-20s %-10s%n",
            "License Plate", "Vehicle Type", "Slot Number", "Entry Time", "Exit Time", "Fee");
        System.out.println("-----------------------------------------------------------------------------------");

        // Iterate through parking records and display details
        for (ParkingRecord record : parkingRecords) {
            String exitTime = (record.getExitTime() != null) 
                    ? record.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                    : "N/A"; // Exit time is null for active records

            String fee = (record.getParkingFee() != null) 
                    ? "₹" + record.getParkingFee() 
                    : "N/A"; // Fee is null for active records

            System.out.printf("%-15s %-12s %-12s %-20s %-20s %-10s%n",
                record.getVehicle().getLicensePlate(),
                record.getVehicle().getVehicleType(),
                record.getParkingSlot().getSlotNumber(),
                record.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                exitTime,
                fee
            );
        }
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("\nTotal Parking Records: " + parkingRecords.size());
    }

    
}