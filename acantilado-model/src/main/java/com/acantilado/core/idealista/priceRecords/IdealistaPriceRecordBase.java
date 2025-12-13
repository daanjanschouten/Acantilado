package com.acantilado.core.idealista.priceRecords;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class IdealistaPriceRecordBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "property_code", nullable = false)
  private Long propertyCode;

  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "recorded_at", nullable = false)
  private Long recordedAt;

  public IdealistaPriceRecordBase() {}

  public IdealistaPriceRecordBase(Long propertyCode, Long price, Long recordedAt) {
    this.propertyCode = propertyCode;
    this.price = price;
    this.recordedAt = recordedAt;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPropertyCode() {
    return propertyCode;
  }

  public void setPropertyCode(Long propertyCode) {
    this.propertyCode = propertyCode;
  }

  public Long getPrice() {
    return price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public Long getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(Long recordedAt) {
    this.recordedAt = recordedAt;
  }
}
