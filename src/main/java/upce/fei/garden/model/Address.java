package upce.fei.garden.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Adresa zahrady – ulice, město a PSČ.
 */

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String city;
    private String street;
    private String houseNumber;
    private String postalCode;
}
