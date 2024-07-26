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

@RestController
public class ConsentFormController {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;



    @PostMapping(value = "/save-consentform", produces = "application/hal+json")
    public void insertConsentForm(@RequestBody ConsentForm consentForm) {

        Integer consentFormId=null;
        if(consentForm.getConsentFormId()==null) {
            String sql = "INSERT INTO consent_form (consent_form_name, form_type, description, html, facility_id, location_id, created_by, modified_by, created_dt, modified_dt) " +
                    "VALUES (:consentFormName, :formType, :description, :html, :facilityId, :locationId, :createdBy, :modifiedBy, :createdDt, :modifiedDt)";

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
                    "facility_id = :facilityId, location_id = :locationId, created_by = :createdBy, modified_by = :modifiedBy, created_dt = :createdDt, modified_dt = :modifiedDt " +
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
        
        
    }
}
