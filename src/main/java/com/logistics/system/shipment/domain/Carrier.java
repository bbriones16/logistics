package com.logistics.system.shipment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "carriers")
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentMedium medium;

    @Column(nullable = false)
    private boolean companyOwned = false;

    @Column
    private String contactName;

    @Column
    private String contactPhone;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ShipmentMedium getMedium() { return medium; }
    public void setMedium(ShipmentMedium medium) { this.medium = medium; }
    public boolean isCompanyOwned() { return companyOwned; }
    public void setCompanyOwned(boolean companyOwned) { this.companyOwned = companyOwned; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
