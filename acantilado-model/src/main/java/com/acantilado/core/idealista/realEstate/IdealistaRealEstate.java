package com.acantilado.core.idealista.realEstate;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.priceRecords.IdealistaPriceRecordBase;
import java.util.List;

public interface IdealistaRealEstate<T extends IdealistaPriceRecordBase> {
    Long getPropertyCode();
    void setPropertyCode(Long propertyCode);

    String getOperation();
    void setOperation(String operation);

    String getDescription();
    void setDescription(String description);

    Long getSize();
    void setSize(Long size);

    String getSubTypology();
    void setSubTypology(String subTypology);

    String getAddress();
    void setAddress(String address);

    String getMunicipality();
    void setMunicipality(String municipality);

    String getLocationId();
    void setLocationId(String locationId);

    String getAcantiladoLocationId();
    void setAcantiladoLocationId(String locationId);

    Double getLatitude();
    void setLatitude(Double latitude);

    Double getLongitude();
    void setLongitude(Double longitude);

    Long getFirstSeen();
    void setFirstSeen(Long firstSeen);

    Long getLastSeen();
    void setLastSeen(Long lastSeen);

    List<T> getPriceRecords();
    void setPriceRecords(List<T> priceRecords);

    IdealistaContactInformation getContactInfo();
    void setContactInfo(IdealistaContactInformation contactInfo);
}