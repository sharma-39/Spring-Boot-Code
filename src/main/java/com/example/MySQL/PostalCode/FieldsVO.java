package com.example.MySQL.PostalCode;

import java.util.List;

public class FieldsVO {
    String fieldName;

    List<Fields> listFields;


    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<Fields> getListFields() {
        return listFields;
    }

    public void setListFields(List<Fields> listFields) {
        this.listFields = listFields;
    }
}
