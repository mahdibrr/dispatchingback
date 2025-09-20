package org.example.dispatcher.dto;

import org.example.common.dto.AddressDto;
import org.example.common.dto.UserDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MissionDto(
	UUID id,
	String reference,
	String status,
	AddressDto pickup,
	AddressDto dropoff,
	Instant createdAt,
	Instant updatedAt,
	Instant assignedAt,
	Instant pickedUpAt,
	Instant inTransitAt,
	Instant deliveredAt,
	String eta,
	BigDecimal priceEstimate,
	UserDto driver,
	String parcelSize, // Taille du colis
	String parcelNotes // Notes additionnelles / Instructions particulières
) {}
