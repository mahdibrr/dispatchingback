package org.example.shared.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "missions")
public class Mission {
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    private Instant assignedAt;
    private Instant pickedUpAt;
    private Instant inTransitAt;
    private Instant deliveredAt;
    
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public Instant getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(Instant pickedUpAt) { this.pickedUpAt = pickedUpAt; }
    public Instant getInTransitAt() { return inTransitAt; }
    public void setInTransitAt(Instant inTransitAt) { this.inTransitAt = inTransitAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "line1", column = @Column(name = "pickup_line1")),
        @AttributeOverride(name = "city", column = @Column(name = "pickup_city")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "pickup_postal_code")),
        @AttributeOverride(name = "notes", column = @Column(name = "pickup_notes")),
        @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "pickup_lng"))
    })
    private Address pickup;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "line1", column = @Column(name = "dropoff_line1")),
        @AttributeOverride(name = "city", column = @Column(name = "dropoff_city")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "dropoff_postal_code")),
        @AttributeOverride(name = "notes", column = @Column(name = "dropoff_notes")),
        @AttributeOverride(name = "lat", column = @Column(name = "dropoff_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "dropoff_lng"))
    })
    private Address dropoff;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
    private Instant eta;
    private BigDecimal priceEstimate;
    
    // Parcel details
    @Column(name = "parcel_size", length = 50)
    private String parcelSize;

    @Column(name = "parcel_notes", columnDefinition = "TEXT")
    private String parcelNotes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }
    public Address getPickup() { return pickup; }
    public void setPickup(Address pickup) { this.pickup = pickup; }
    public Address getDropoff() { return dropoff; }
    public void setDropoff(Address dropoff) { this.dropoff = dropoff; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getEta() { return eta; }
    public void setEta(Instant eta) { this.eta = eta; }
    public BigDecimal getPriceEstimate() { return priceEstimate; }
    public void setPriceEstimate(BigDecimal priceEstimate) { this.priceEstimate = priceEstimate; }
    public String getParcelSize() { return parcelSize; }
    public void setParcelSize(String parcelSize) { this.parcelSize = parcelSize; }
    public String getParcelNotes() { return parcelNotes; }
    public void setParcelNotes(String parcelNotes) { this.parcelNotes = parcelNotes; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
