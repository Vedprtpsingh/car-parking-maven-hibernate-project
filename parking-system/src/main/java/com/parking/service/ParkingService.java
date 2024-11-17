package com.parking.service;

import com.parking.dao.ParkingDAO;
import com.parking.model.ParkingSpot;
import com.parking.model.ParkingTransaction;
import com.parking.model.Vehicle;
import com.parking.util.HibernateUtil;

public class ParkingService {
    private ParkingDAO parkingDAO;

    public ParkingService() {
        this.parkingDAO = new ParkingDAO(HibernateUtil.getSessionFactory());
    }

    public ParkingTransaction parkVehicle(String licensePlate, String vehicleType) throws Exception {
        ParkingSpot spot = parkingDAO.findAvailableSpot(vehicleType);
        if (spot == null) {
            throw new Exception("No parking spot available for " + vehicleType);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(licensePlate);
        vehicle.setVehicleType(vehicleType);

        return parkingDAO.parkVehicle(vehicle, spot);
    }

    public ParkingTransaction exitParking(Long transactionId) throws Exception {
        return parkingDAO.exitParking(transactionId);
    }

    public void initializeParkingSpots() {
        parkingDAO.initializeParkingSpots();
    }
}