
package com.ved.dao;
import com.ved.model.Vehicle;
import com.ved.model.ParkingSlot;
import com.ved.model.ParkingRecord;
import com.ved.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ParkingManagementDAO {
    
    public void saveVehicle(Vehicle vehicle) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(vehicle);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public void saveParkingSlot(ParkingSlot slot) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(slot);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public ParkingRecord createParkingRecord(Vehicle vehicle, ParkingSlot slot) {
        Transaction transaction = null;
        ParkingRecord record = new ParkingRecord();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            record.setVehicle(vehicle);
            record.setParkingSlot(slot);
            record.setEntryTime(LocalDateTime.now());
            
            slot.setOccupied(true);
            session.update(slot);
            session.save(record);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
        return record;
    }

    public void completeParkingRecord(ParkingRecord record) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            record.setExitTime(LocalDateTime.now());
            record.setParkingFee(calculateParkingFee(record));
            
            ParkingSlot slot = record.getParkingSlot();
            slot.setOccupied(false);
            
            session.update(slot);
            session.update(record);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    private double calculateParkingFee(ParkingRecord record) {
        long hours = ChronoUnit.HOURS.between(record.getEntryTime(), record.getExitTime());
        // Basic fee calculation: ₹200 per hour
        return Math.max(200.0, hours * 2.0);
    }

    public List<ParkingSlot> getAvailableSlots() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ParkingSlot WHERE occupied = false", ParkingSlot.class).list();
        }
    }
    
    
    public Vehicle findVehicleByLicensePlate(String licensePlate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Vehicle WHERE licensePlate = :licensePlate", 
                    Vehicle.class)
                    .setParameter("licensePlate", licensePlate)
                    .uniqueResult();
        }
    }

    public ParkingSlot findParkingSlotById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(ParkingSlot.class, id);
        }
    }

    public ParkingRecord findActiveParkingRecord(String licensePlate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM ParkingRecord pr " +
                    "WHERE pr.vehicle.licensePlate = :licensePlate " +
                    "AND pr.exitTime IS NULL", 
                    ParkingRecord.class)
                    .setParameter("licensePlate", licensePlate)
                    .uniqueResult();
        }
    }
    public List<ParkingRecord> getActiveParkingRecords() {
        Transaction transaction = null;
        List<ParkingRecord> activeRecords = new ArrayList<>();
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Start transaction
            transaction = session.beginTransaction();
            
            // Create query to get all parking records where exitTime is null (active parkings)
            String hql = "FROM ParkingRecord pr " +
                        "LEFT JOIN FETCH pr.vehicle " + 
                        "LEFT JOIN FETCH pr.parkingSlot " + 
                        "WHERE pr.exitTime IS NULL " +
                        "ORDER BY pr.entryTime DESC";
            
            activeRecords = session.createQuery(hql, ParkingRecord.class)
                                  .list();
            
            // Commit transaction
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("Error getting active parking records: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activeRecords;
    }
    
    public int getTotalParkingSlots() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(*) FROM ParkingSlot", Long.class)
                               .uniqueResult();
            return count.intValue();
        }
    }

    public List<ParkingSlot> getAllParkingSlots() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ParkingSlot ORDER BY slotNumber", ParkingSlot.class)
                         .list();
        }
    }

    public void updateParkingSlot(ParkingSlot slot) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(slot);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public List<Vehicle> getAllVehicles() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Vehicle ORDER BY licensePlate", Vehicle.class)
                         .list();
        }
    }

    public boolean isVehicleParked(Long vehicleId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                "SELECT COUNT(*) FROM ParkingRecord " +
                "WHERE vehicle.id = :vehicleId AND exitTime IS NULL", 
                Long.class)
                .setParameter("vehicleId", vehicleId)
                .uniqueResult();
            return count > 0;
        }
    }
    
    public boolean canParkMoreVehicles(String vehicleType) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Count total slots of this type
            Long totalSlots = session.createQuery(
                "SELECT COUNT(*) FROM ParkingSlot WHERE slotType = :type",
                Long.class)
                .setParameter("type", vehicleType)
                .uniqueResult();
                
            // Count occupied slots of this type
            Long occupiedSlots = session.createQuery(
                "SELECT COUNT(*) FROM ParkingSlot WHERE slotType = :type AND occupied = true",
                Long.class)
                .setParameter("type", vehicleType)
                .uniqueResult();
                
            return occupiedSlots < totalSlots;
        }
    }
    
}