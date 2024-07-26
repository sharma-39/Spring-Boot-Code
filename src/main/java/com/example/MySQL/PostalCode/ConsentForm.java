package com.example.MySQL.PostalCode;

import java.sql.Timestamp;
import java.util.List;

public class ConsentForm {
    private Integer consentFormId;
    private String consentFormName;
    private String formType;
    private String description;
    private String html;
    private Integer facilityId;
    private Integer locationId;
    private String createdBy;
    private String modifiedBy;
    private Timestamp createdDt;
    private Timestamp modifiedDt;
    List<ConsentFormAssoc> htmlFields;
    List<Integer> deleteFieldAssocIds;

    public Integer getConsentFormId() {
        return consentFormId;
    }

    public void setConsentFormId(Integer consentFormId) {
        this.consentFormId = consentFormId;
    }

    public String getConsentFormName() {
        return consentFormName;
    }

    public void setConsentFormName(String consentFormName) {
        this.consentFormName = consentFormName;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public Integer getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Integer facilityId) {
        this.facilityId = facilityId;
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Timestamp getCreatedDt() {
        return createdDt;
    }

    public void setCreatedDt(Timestamp createdDt) {
        this.createdDt = createdDt;
    }

    public Timestamp getModifiedDt() {
        return modifiedDt;
    }

    public void setModifiedDt(Timestamp modifiedDt) {
        this.modifiedDt = modifiedDt;
    }

    public List<ConsentFormAssoc> getHtmlFields() {
        return htmlFields;
    }

    public void setHtmlFields(List<ConsentFormAssoc> htmlFields) {
        this.htmlFields = htmlFields;
    }

    public List<Integer> getDeleteFieldAssocIds() {
        return deleteFieldAssocIds;
    }

    public void setDeleteFieldAssocIds(List<Integer> deleteFieldAssocIds) {
        this.deleteFieldAssocIds = deleteFieldAssocIds;
    }
}
