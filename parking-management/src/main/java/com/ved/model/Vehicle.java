
package com.ved.model;

import javax.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String vehicleDriverName;

    @Column(nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private String vehicleType;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

	public String getVehicleDriverName() {
		return vehicleDriverName;
	}

	public void setVehicleDriverName(String vehicleDriverName) {
		this.vehicleDriverName = vehicleDriverName;
	}
}