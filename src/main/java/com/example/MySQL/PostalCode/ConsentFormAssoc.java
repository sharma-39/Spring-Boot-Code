package com.example.MySQL.PostalCode;

public class ConsentFormAssoc {
    private Integer consentFormAssocId;
    private Integer consentFormId;
    private Integer fieldTypeId;
    private String fieldLabelName;
    private boolean isRequired;
    private boolean sizeRequired;
    private Integer maxSize;
    private Integer minSize;
    private String type;
    private String fieldValue;
    private String fieldCode;

    public Integer getConsentFormAssocId() {
        return consentFormAssocId;
    }

    public void setConsentFormAssocId(Integer consentFormAssocId) {
        this.consentFormAssocId = consentFormAssocId;
    }

    public Integer getConsentFormId() {
        return consentFormId;
    }

    public void setConsentFormId(Integer consentFormId) {
        this.consentFormId = consentFormId;
    }

    public Integer getFieldTypeId() {
        return fieldTypeId;
    }

    public void setFieldTypeId(Integer fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

    public String getFieldLabelName() {
        return fieldLabelName;
    }

    public void setFieldLabelName(String fieldLabelName) {
        this.fieldLabelName = fieldLabelName;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isSizeRequired() {
        return sizeRequired;
    }

    public void setSizeRequired(boolean sizeRequired) {
        this.sizeRequired = sizeRequired;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }
}
