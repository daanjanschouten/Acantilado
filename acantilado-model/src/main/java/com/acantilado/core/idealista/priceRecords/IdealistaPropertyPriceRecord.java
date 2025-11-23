package com.acantilado.core.idealista.priceRecords;

import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "idealista_property_price_records")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaPropertyPriceRecord.findByPropertyCode",
                        query = "SELECT p FROM IdealistaPropertyPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaPropertyPriceRecord.findLatestByPropertyCode",
                        query = "SELECT p FROM IdealistaPropertyPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                )
        }
)
public class IdealistaPropertyPriceRecord extends IdealistaPriceRecordBase {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_code", referencedColumnName = "property_code", insertable = false, updatable = false)
    private IdealistaProperty property;

    public IdealistaPropertyPriceRecord() {}

    public IdealistaPropertyPriceRecord(Long propertyCode, Long price, Long recordedAt) {
        super(propertyCode, price, recordedAt);
    }

    public IdealistaProperty getProperty() { return property; }
    public void setProperty(IdealistaProperty property) { this.property = property; }
}