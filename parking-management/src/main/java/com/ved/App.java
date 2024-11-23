
package com.ved;

import com.ved.dao.ParkingManagementDAO;
import com.ved.model.Vehicle;
import com.ved.model.ParkingRecord;
import com.ved.model.ParkingSlot;

import java.util.Scanner;
import java.util.List;

public class App {
    private static final ParkingManagementDAO dao = new ParkingManagementDAO();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
    	while (true) {
            System.out.println("\nParking Management System");
            System.out.println("1. Add New Vehicle");
            System.out.println("2. Add Parking Slot");
            System.out.println("3. Park Vehicle");
            System.out.println("4. Remove Vehicle");
            System.out.println("5. View Available Slots");
            System.out.println("6. Show Parked Vehicles");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addNewVehicle();
                    break;
                case 2:
                    addParkingSlot();
                    break;
                case 3:
                    parkVehicle();
                    break;
                case 4:
                    removeVehicle();
                    break;
                case 5:
                    viewAvailableSlots();
                    break;
                case 6:
                    showParkedVehicles();
                    break;
                case 7:
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private static void addNewVehicle() {
        Vehicle vehicle = new Vehicle();
        System.out.print("Enter license plate: ");
        vehicle.setLicensePlate(scanner.nextLine());
        System.out.print("Enter vehicle type (CAR/BIKE): ");
        vehicle.setVehicleType(scanner.nextLine());
        dao.saveVehicle(vehicle);
        System.out.println("Vehicle added successfully!");
    }

    private static void addParkingSlot() {
        ParkingSlot slot = new ParkingSlot();
        System.out.print("Enter slot number: ");
        slot.setSlotNumber(scanner.nextLine());
        System.out.print("Enter slot type (CAR/BIKE): ");
        slot.setSlotType(scanner.nextLine());
        slot.setOccupied(false);
        dao.saveParkingSlot(slot);
        System.out.println("Parking slot added successfully!");
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
            System.out.println("Parking Fee: $" + record.getParkingFee());
            
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
        List<ParkingRecord> parkedVehicles = dao.getActiveParkingRecords();
        
        if (parkedVehicles.isEmpty()) {
            System.out.println("No vehicles are currently parked.");
            return;
        }
        
        for (ParkingRecord record : parkedVehicles) {
            Vehicle vehicle = record.getVehicle();
            ParkingSlot slot = record.getParkingSlot();
            System.out.println("License Plate: " + vehicle.getLicensePlate() +
                               ", Type: " + vehicle.getVehicleType() +
                               ", Slot Number: " + slot.getSlotNumber() +
                               ", Entry Time: " + record.getEntryTime());
        }
    }
}