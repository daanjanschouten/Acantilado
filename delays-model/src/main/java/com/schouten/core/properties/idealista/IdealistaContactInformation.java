package com.schouten.core.properties.idealista;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "IDEALISTA_CONTACT_INFORMATION")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaContactInformation.findAll",
                        query = "SELECT c FROM IdealistaContactInformation c"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaContactInformation.findByPropertyCode",
                        query = "SELECT c FROM IdealistaContactInformation c WHERE c.propertyCode = :propertyCode"
                )
        }
)
public class IdealistaContactInformation {
    @Id
    @Column(name = "phone_number")
    private long phone_number;

    @Column(name = "prefix")
    private long prefix;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "user_type")
    private String userType;

    @OneToMany(mappedBy = "contactInfo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<IdealistaProperty> properties = new HashSet<>();

    public IdealistaContactInformation() {}

    public IdealistaContactInformation(long phone_number, long prefix, String contactName, String userType) {
        this.phone_number = phone_number;
        this.prefix = prefix;
        this.contactName = contactName;
        this.userType = userType;
    }

    public long getId() {
        return phone_number;
    }

    public long getPrefix() {
        return prefix;
    }

    public String getContactName() {
        return contactName;
    }

    public String getUserType() {
        return userType;
    }

    public Set<IdealistaProperty> getProperties() {
        return properties;
    }

    public void setId(long phone_number) {
        this.phone_number = phone_number;
    }

    public void setPrefix(long prefix) {
        this.prefix = prefix;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setProperties(Set<IdealistaProperty> properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phone_number, prefix, contactName, userType, properties);
    }

    @Override
    public String toString() {
        return "IdealistaContactInformation{" +
                "phone_number=" + phone_number +
                ", prefix='" + prefix + '\'' +
                ", contactName='" + contactName + '\'' +
                ", userType='" + userType + '\'' +
                ", properties=" + properties +
                '}';
    }
}