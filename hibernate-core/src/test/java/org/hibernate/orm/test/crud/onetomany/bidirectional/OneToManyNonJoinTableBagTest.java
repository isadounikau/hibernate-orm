/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud.onetomany.bidirectional;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hamcrest.CoreMatchers;
import org.hibernate.boot.MetadataSources;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class OneToManyNonJoinTableBagTest extends SessionFactoryBasedFunctionalTest {
	@Entity(name = "Owner")
	public static class Owner {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany
		@JoinColumn(name = "owner_id", referencedColumnName = "id")
		private List<Car> cars = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Car> getCars() {
			return cars;
		}

		public void setCars(List<Car> cars) {
			this.cars = cars;
		}
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Owner.class );
		metadataSources.addAnnotatedClass( Car.class );
	}

	private Integer carId1;
	private Integer carId2;
	private Integer ownerId1;
	private Integer ownerId2;
	private Integer ownerId3;

	@Test
	public void testOperations() {
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car1 = new Car();
					session.save( car1 );
					this.carId1 =  car1.getId();

					final Car car2 = new Car();
					session.save( car2 );
					this.carId2 =  car2.getId();
				}
		);

		// Save without a car
		sessionFactoryScope().inTransaction(
				session -> {
					final Owner owner = new Owner();
					session.save( owner );
					this.ownerId1 = owner.getId();
				}
		);

		// Save with 1 car
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car = session.find( Car.class, this.carId1 );

					final Owner owner = new Owner();
					owner.getCars().add( car );
					session.save( owner );
					this.ownerId2 = owner.getId();
				}
		);

		// Save with 2 cars
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car1 = session.find( Car.class, this.carId1 );
					final Car car2 = session.find( Car.class, this.carId2 );

					final Owner owner = new Owner();
					owner.getCars().add( car1 );
					owner.getCars().add( car2 );
					session.save( owner );
					this.ownerId3 = owner.getId();
				}
		);

		// Update owner, add car to empty collection
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car1 = session.find( Car.class, this.carId1 );
					final Owner owner = session.find( Owner.class, this.ownerId1 );
					owner.getCars().add( car1 );
					session.update( owner );
				}
		);

		// Update owner, add car to non-empty collection
		sessionFactoryScope().inTransaction(
				session -> {
					final Car car1 = session.find( Car.class, this.carId1 );
					final Owner owner = session.find( Owner.class, this.ownerId2 );
					owner.getCars().add( car1 );
					session.update( owner );
				}
		);

		// Test removing element from collection
		sessionFactoryScope().inTransaction(
				session -> {
					final Owner owner = session.find( Owner.class, this.ownerId2 );
					owner.getCars().removeIf( car -> car.getId().equals( this.carId1 ) );
					session.update( owner );
				}
		);

		// Test setting collection to new collection
//		sessionFactoryScope().inTransaction(
//				session -> {
//					final Owner owner = session.find( Owner.class, this.ownerId1 );
//					owner.setCars( new ArrayList<>() );
//					session.update( owner );
//				}
//		);

		sessionFactoryScope().inTransaction(
				session -> {
//					final Owner owner1 = session.find( Owner.class, this.ownerId1 );
//					assertThat( owner1.getCars().isEmpty(), CoreMatchers.is( true ) );

					final Owner owner2 = session.find( Owner.class, this.ownerId2 );
					assertThat( owner2.getCars().isEmpty(), CoreMatchers.is( true ) );

					final Owner owner3 = session.find( Owner.class, this.ownerId3 );
					assertThat( owner3.getCars().isEmpty(), CoreMatchers.is( false ) );
					assertThat( owner3.getCars().size(), CoreMatchers.is( 2 ) );
				}
		);
	}

}
