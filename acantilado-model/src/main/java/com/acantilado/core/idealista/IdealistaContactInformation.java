package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "idealista_contact_information")
@NamedQueries({
  @NamedQuery(
      name = "com.schouten.core.properties.idealista.IdealistaContactInformation.findAll",
      query = "SELECT c FROM IdealistaContactInformation c")
})
public class IdealistaContactInformation {
  public record PhoneContact(long phonePrefix, long phoneNumber) {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private long id;

  @Column(name = "phone_number")
  private long phone_number;

  @Column(name = "prefix")
  private long prefix;

  @Column(name = "contact_name")
  private String contactName;

  @Column(name = "user_type")
  private String userType;

  @JsonIgnore
  @OneToMany(mappedBy = "contactInfo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<IdealistaProperty> properties = new HashSet<>();

  @JsonIgnore
  @OneToMany(mappedBy = "contactInfo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<IdealistaTerrain> terrains = new HashSet<>();

  public IdealistaContactInformation() {}

  public IdealistaContactInformation(
      Optional<PhoneContact> phoneContact, String contactName, String userType) {
    this.contactName = contactName;
    this.userType = userType;

    phoneContact.ifPresent(
        c -> {
          this.prefix = c.phonePrefix;
          this.phone_number = c.phoneNumber;
        });
  }

  public long getId() {
    return id;
  }

  public long getPhoneNumber() {
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

  public Set<IdealistaTerrain> getTerrains() {
    return terrains;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setPhoneNumber(long phone_number) {
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

  public void setTerrains(Set<IdealistaTerrain> terrains) {
    this.terrains = terrains;
  }

  @Override
  public String toString() {
    return "IdealistaContactInformation{"
        + "id="
        + id
        + ", phone_number="
        + phone_number
        + ", prefix="
        + prefix
        + ", contactName='"
        + contactName
        + '\''
        + ", userType='"
        + userType
        + '\''
        + ", terrains='"
        + terrains
        + '\''
        + ", properties='"
        + properties
        + '\''
        + '}';
  }
}
