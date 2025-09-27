package com.schouten.core.properties.idealista;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idealista_price_records")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaPriceRecord.findByPropertyCode",
                        query = "SELECT p FROM IdealistaPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaPriceRecord.findLatestByPropertyCode",
                        query = "SELECT p FROM IdealistaPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                )
        }
)
public class IdealistaPriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "property_code", nullable = false)
    private long propertyCode;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_code", referencedColumnName = "property_code", insertable = false, updatable = false)
    private IdealistaProperty property;

    public IdealistaPriceRecord() {}

    public IdealistaPriceRecord(Long propertyCode, Long price, LocalDateTime recordedAt) {
        this.propertyCode = propertyCode;
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropertyCode() { return propertyCode; }
    public void setPropertyCode(Long propertyCode) { this.propertyCode = propertyCode; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public IdealistaProperty getProperty() { return property; }
    public void setProperty(IdealistaProperty property) { this.property = property; }
}