package com.acantilado.core.idealista.priceRecords;

import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;


@Entity
@Table(name = "idealist_terrain_price_records")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaTerrainPriceRecord.findByPropertyCode",
                        query = "SELECT p FROM IdealistaTerrainPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaTerrainPriceRecord.findLatestByPropertyCode",
                        query = "SELECT p FROM IdealistaTerrainPriceRecord p WHERE p.propertyCode = :propertyCode ORDER BY p.recordedAt DESC"
                )
        }
)
public class IdealistaTerrainPriceRecord extends IdealistaPriceRecordBase {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_code", referencedColumnName = "property_code", insertable = false, updatable = false)
    private IdealistaTerrain terrain;

    public IdealistaTerrainPriceRecord() {}

    public IdealistaTerrainPriceRecord(Long propertyCode, Long price, Long recordedAt) {
        super(propertyCode, price, recordedAt);
    }

    public IdealistaTerrain getTerrain() { return terrain; }
    public void setTerrain(IdealistaTerrain terrain) { this.terrain = terrain; }
}
