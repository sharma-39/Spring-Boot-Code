package com.example.MySQL.PostalCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ConsentFormController {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @GetMapping(value="/fields", produces = "application/hal+json")
    public List<FieldsVO> getDropDownFields( )
    {
        String sql = "SELECT * FROM field_type where active=1"; // Replace with your actual query


        List<Fields> fieldTypeList = namedParameterJdbcTemplate.query(sql,Map.of(),ConsentFormController::toListFields);

       List<FieldsVO> fieldsVO =new ArrayList<>();

        Map<String, List<Fields>> fieldsList = fieldTypeList.stream()
                .collect(Collectors.groupingBy(Fields::getType, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Fields>> entry : fieldsList.entrySet()) {

            FieldsVO fieldsVO1=new FieldsVO();
            fieldsVO1.setFieldName(entry.getKey());
            fieldsVO1.setListFields(entry.getValue());
            fieldsVO.add(fieldsVO1);

        }
        return fieldsVO;

    }

    private static Fields toListFields(ResultSet resultSet, int i) throws SQLException {
        Fields fieldType= new Fields();
        fieldType.setFieldTypeId(resultSet.getInt("field_type_id"));
        fieldType.setFieldType(resultSet.getString("field_type"));
        fieldType.setType(resultSet.getString("type"));
        fieldType.setLabelName(resultSet.getString("label_name"));
        return fieldType;
    }


    @PostMapping(value = "/save-consentform", produces = "application/hal+json")
    public void insertConsentForm(@RequestBody ConsentForm consentForm) {

        Integer consentFormId=null;
        if(consentForm.getConsentFormId()==null) {
            String sql = "INSERT INTO consent_form (consent_form_name, form_type, description, html, facility_id, location_id, created_by, modified_by, created_dt, modified_dt) " +
                    "VALUES (:consentFormName, :formType, :description, :html, :facilityId, :locationId, :createdBy, :modifiedBy, now(), now())";

            KeyHolder keyHolder=new GeneratedKeyHolder();
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("consentFormName", consentForm.getConsentFormName());
            params.addValue("formType", consentForm.getFormType());
            params.addValue("description", consentForm.getDescription());
            params.addValue("html", consentForm.getHtml());
            params.addValue("facilityId", consentForm.getFacilityId());
            params.addValue("locationId", consentForm.getLocationId());
            params.addValue("createdBy", consentForm.getCreatedBy());
            params.addValue("modifiedBy", consentForm.getModifiedBy());
            params.addValue("createdDt", consentForm.getCreatedDt());
            params.addValue("modifiedDt", consentForm.getModifiedDt());

            namedParameterJdbcTemplate.update(sql, params,keyHolder);
            consentFormId= keyHolder.getKey().intValue();

        }
        else {
            String sql = "UPDATE consent_form SET consent_form_name = :consentFormName, form_type = :formType, description = :description, html = :html, " +
                    "facility_id = :facilityId, location_id = :locationId, modified_by = :modifiedBy, modified_dt = now() " +
                    "WHERE consent_form_id = :consentFormId";

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("consentFormName", consentForm.getConsentFormName());
            params.addValue("formType", consentForm.getFormType());
            params.addValue("description", consentForm.getDescription());
            params.addValue("html", consentForm.getHtml());
            params.addValue("facilityId", consentForm.getFacilityId());
            params.addValue("locationId", consentForm.getLocationId());
            params.addValue("createdBy", consentForm.getCreatedBy());
            params.addValue("modifiedBy", consentForm.getModifiedBy());
            params.addValue("createdDt", consentForm.getCreatedDt());
            params.addValue("modifiedDt", consentForm.getModifiedDt());
            params.addValue("consentFormId", consentForm.getConsentFormId());

            namedParameterJdbcTemplate.update(sql, params);
            consentFormId= consentForm.getConsentFormId();
        }

        if(consentForm.getConsentFormId()==null)
        {
            createFormFields(consentForm.getHtmlFields(),consentFormId,consentForm);
        }
        else {
            if (consentForm.getDeleteFieldAssocIds() != null && consentForm.getDeleteFieldAssocIds().size() != 0) {

            }
            updateFields(consentForm.getHtmlFields(),consentFormId,consentForm);
        }

        
        
    }

    private void updateFields(List<ConsentFormAssoc> htmlFields, Integer consentFormId, ConsentForm consentForm) {
    }

    private void createFormFields(List<ConsentFormAssoc> htmlFields, Integer consentFormId, ConsentForm consentForm) {
        String parentQuery = "INSERT INTO consent_form_assoc (consent_form_id, field_type_id, field_label_name, is_required, size_required, max_size, min_size, type, field_value, field_code) VALUES " +
                " %multipleInsert%";
        String queryFormation = "";

        List<ConsentFormAssoc> fields = htmlFields;
        int c = 0;
        for (int i = 0; i < fields.size(); i++) {
            ConsentFormAssoc single = fields.get(i);
                c++;


            String uniqueCode = "";
            if(single.getType().toLowerCase()!="custom") {
                uniqueCode = generateUniqueCode();
            }
            else {
                uniqueCode=single.getFieldCode();
            }
            String query = "" +
                    " ("+consentFormId+", "+single.getFieldTypeId()+", '"+single.getFieldLabelName()+"', "+single.isRequired()+", "+single.isSizeRequired()+", "+single.getMaxSize()+", "+single.getMinSize()+", '"+single.getType()+"', "+single.getFieldValue()+", '"+uniqueCode+"'),";
                queryFormation = queryFormation + query;
            single = new ConsentFormAssoc() ;
        }
        if (c != 0) {
            queryFormation = queryFormation.substring(0, queryFormation.length() - 1);
            parentQuery = parentQuery.replace("%multipleInsert%", queryFormation);
            namedParameterJdbcTemplate.update(parentQuery, Map.of("facilityId", consentForm.getFacilityId(), "locationId", consentForm.getLocationId(), "createdBy", consentForm.getCreatedBy()));
        }

    }

    private String generateUniqueCode() {
        String uniqueCode = "";
        boolean isUnique = false;
        
        while (!isUnique) {
            uniqueCode = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String sql = "SELECT COUNT(*) FROM consent_form_assoc WHERE field_code = :fieldCode";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("fieldCode", uniqueCode);

            int count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);

            if (count == 0) {
                isUnique = true;
                return uniqueCode;
            }
        }
     return uniqueCode;
    }
}
