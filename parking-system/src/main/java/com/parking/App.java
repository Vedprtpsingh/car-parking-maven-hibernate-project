package com.parking;
import com.parking.service.ParkingService;
import com.parking.model.ParkingTransaction;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        ParkingService parkingService = new ParkingService();
        Scanner scanner = new Scanner(System.in);
        try {
            // Initialize parking spots
            parkingService.initializeParkingSpots();
            
            while (true) {
                System.out.println("\n=== Parking Management System ===");
                System.out.println("1. Park Vehicle");
                System.out.println("2. Exit Parking");
                System.out.println("3. Quit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        parkVehicle(scanner, parkingService);
                        break;
                    case 2:
                        exitParking(scanner, parkingService);
                        break;
                    case 3:
                        System.out.println("Thank you for using Parking Management System!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void parkVehicle(Scanner scanner, ParkingService parkingService) {
        try {
            System.out.print("Enter vehicle license plate: ");
            String licensePlate = scanner.nextLine();

            System.out.print("Enter vehicle type (CAR/BIKE/TRUCK): ");
            String vehicleType = scanner.nextLine().toUpperCase();

            ParkingTransaction transaction = parkingService.parkVehicle(licensePlate, vehicleType);
            System.out.println("Vehicle parked successfully!");
            System.out.println("Transaction ID: " + transaction.getId());
            System.out.println("Spot Number: " + transaction.getParkingSpot().getSpotNumber());
            System.out.println("Entry Time: " + transaction.getEntryTime());
        } catch (Exception e) {
            System.err.println("Error parking vehicle: " + e.getMessage());
        }
    }

    private static void exitParking(Scanner scanner, ParkingService parkingService) {
        try {
            System.out.print("Enter transaction ID: ");
            Long transactionId = scanner.nextLong();

            ParkingTransaction transaction = parkingService.exitParking(transactionId);
            if (transaction != null) {
                System.out.println("Vehicle exit processed successfully!");
                System.out.println("Exit Time: " + transaction.getExitTime());
                System.out.println("Parking Fee: $" + transaction.getFee());
            } else {
                System.out.println("Transaction not found.");
            }
        } catch (Exception e) {
            System.err.println("Error processing exit: " + e.getMessage());
        }
    }
}