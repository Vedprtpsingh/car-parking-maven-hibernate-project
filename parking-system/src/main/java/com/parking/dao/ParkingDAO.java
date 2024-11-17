package com.parking.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import com.parking.model.ParkingSpot;
import com.parking.model.Vehicle;
import com.parking.model.ParkingTransaction;
import java.time.LocalDateTime;
import java.util.List;

public class ParkingDAO {
    private final SessionFactory sessionFactory;
    
    public ParkingDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public ParkingSpot findAvailableSpot(String vehicleType) {
        try (Session session = sessionFactory.openSession()) {
            Query<ParkingSpot> query = session.createQuery(
                "FROM ParkingSpot WHERE isOccupied = false AND spotType = :type",
                ParkingSpot.class
            );
            query.setParameter("type", vehicleType);
            query.setMaxResults(1);
            return query.uniqueResult();
        }
    }
    
    public ParkingTransaction parkVehicle(Vehicle vehicle, ParkingSpot spot) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            // Save vehicle if new
            session.saveOrUpdate(vehicle);
            
            // Update parking spot
            spot.setOccupied(true);
            session.update(spot);
            
            // Create parking transaction
            ParkingTransaction parkingTransaction = new ParkingTransaction();
            parkingTransaction.setVehicle(vehicle);
            parkingTransaction.setParkingSpot(spot);
            parkingTransaction.setEntryTime(LocalDateTime.now());
            
            session.save(parkingTransaction);
            transaction.commit();
            return parkingTransaction;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    public ParkingTransaction exitParking(Long transactionId) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            
            ParkingTransaction parkingTransaction = session.get(ParkingTransaction.class, transactionId);
            if (parkingTransaction != null && parkingTransaction.getExitTime() == null) {
                // Set exit time
                parkingTransaction.setExitTime(LocalDateTime.now());
                
                // Calculate fee (example: $2 per hour)
                long hours = java.time.Duration.between(
                    parkingTransaction.getEntryTime(),
                    parkingTransaction.getExitTime()
                ).toHours();
                parkingTransaction.setFee(hours * 2.0);
                
                // Free up the parking spot
                ParkingSpot spot = parkingTransaction.getParkingSpot();
                spot.setOccupied(false);
                session.update(spot);
                
                session.update(parkingTransaction);
            }
            
            transaction.commit();
            return parkingTransaction;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    public void initializeParkingSpots() {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // Check if spots already exist
            Query<Long> countQuery = session.createQuery("SELECT COUNT(p) FROM ParkingSpot p", Long.class);
            if (countQuery.uniqueResult() == 0) {
                // Create some initial parking spots
                String[] carSpots = {"A1", "A2", "A3", "A4", "A5"};
                String[] bikeSpots = {"B1", "B2", "B3"};
                String[] truckSpots = {"T1", "T2"};

                // Add car spots
                for (String spotNumber : carSpots) {
                    ParkingSpot spot = new ParkingSpot();
                    spot.setSpotNumber(spotNumber);
                    spot.setSpotType("CAR");
                    spot.setOccupied(false);
                    session.save(spot);
                }

                // Add bike spots
                for (String spotNumber : bikeSpots) {
                    ParkingSpot spot = new ParkingSpot();
                    spot.setSpotNumber(spotNumber);
                    spot.setSpotType("BIKE");
                    spot.setOccupied(false);
                    session.save(spot);
                }

                // Add truck spots
                for (String spotNumber : truckSpots) {
                    ParkingSpot spot = new ParkingSpot();
                    spot.setSpotNumber(spotNumber);
                    spot.setSpotType("TRUCK");
                    spot.setOccupied(false);
                    session.save(spot);
                }
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
}