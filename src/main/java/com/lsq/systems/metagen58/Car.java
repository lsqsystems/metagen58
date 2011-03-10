/*
 * @(#)Account.java Copyright 2011 LSQ Systems, Inc. All rights reserved.
 */
package com.lsq.systems.metagen58;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The {@link Car} entity.
 * 
 * @author Viet Trinh
 * @since 0.7
 */
@Entity
@Table(name = "Car")
@Auditable
public class Car {

	protected Integer _car_id;

	protected String _car_make;

	protected String _car_model;

	public Car() {

	}

	@Id
	@GeneratedValue
	@Column(name = "carId")
	public Integer getCarId() {
		return _car_id;
	}

	public void setCarId(Integer car_id) {
		_car_id = car_id;
	}

	@Column(name = "carMake")
	public String getCarMake() {
		return _car_make;
	}

	public void setCarMake(String car_make) {
		_car_make = car_make;
	}

	@Column(name = "carModel")
	public String getCarModel() {
		return _car_model;
	}

	public void setCarModel(String car_model) {
		_car_model = car_model;
	}

}