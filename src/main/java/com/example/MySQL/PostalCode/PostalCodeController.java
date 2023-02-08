package com.example.MySQL.PostalCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PostalCodeController {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public static void fileOverite(String fileName,
                                   String key, String postalCode) {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));

            out.write("\nupdate postal_map set short= '" + key + "'  where postal_code in(" + postalCode + ");\n");
            // Closing the connection
            out.close();
        }

        // Catch block to handle the exceptions
        catch (IOException e) {

            // Display message when exception occurs
            System.out.println("exception occurred" + e);
        }
    }

    @RequestMapping("/timezone")
    public String timezoneApi() {

        List<String> dublicateApp = new ArrayList<>();
        String fileName = CreateUpdateFile();
        HashMap<String, List<String>> newName = new HashMap<>();
        String Query = "select  postal_id,Time_Zone_Abbr as appr,Time_Zone_Offset_from_api as offset,Postal_Code as postalcode  " +
                "from time_zone_data where Time_Zone_Offset_from_api is not null and Time_Zone_Offset_from_api!=\"\";";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(Query);
        List<EntityModule> newEntity = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map row = rows.get(i);
            String offset = (String) row.get("offset");

            ZoneId zone = ZoneId.of(offset);

            offset = zone.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH);
            System.out.println("" + offset);

            DateTimeFormatter zoneAbbreviationFormatter
                    = DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH);
            offset = ZonedDateTime.now(zone).format(zoneAbbreviationFormatter);
            String postalcode = (String.valueOf((String) row.get("postalcode")));
            EntityModule e = new EntityModule(postalcode, offset);
            newEntity.add(e);
        }

        int sum = 0;
        Map<String, List<EntityModule>> finalData1 = newEntity.stream().collect(Collectors.groupingBy(EntityModule::getTimezoneApp));
        for (Map.Entry<String, List<EntityModule>> entry : finalData1.entrySet()) {
            List<String> postalNewCode = new ArrayList<>();
            for (int j = 0; j < entry.getValue().size(); j++) {
                postalNewCode.add(entry.getValue().get(j).getPostalCode());
            }
            String postalCodeNew = postalNewCode.toString().replace("[", "").replace("]", "");
            fileOverite(fileName, entry.getKey(), postalCodeNew);
            sum = sum + entry.getValue().size();

        }
        return "TotalSize" + sum;
    }

    @RequestMapping("/questTcare5")
    public String timezone() {

        List<String> dublicateApp = new ArrayList<>();
        String fileName = CreateUpdateFile();
        HashMap<String, List<String>> newName = new HashMap<>();
        String Query = "select  postal_id,Time_Zone_Abbr as appr,Time_Zone_Offset_from_api as offset,Postal_Code as postalcode  " +
                "from time_zone_data where Time_Zone_Offset_from_api is not null and Time_Zone_Offset_from_api!=\"\";";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(Query);
        List<EntityModule> newEntity = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map row = rows.get(i);
            String offset = (String) row.get("offset");

            ZoneId zone = ZoneId.of(offset);

            offset = zone.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH);
            System.out.println("" + offset);

            DateTimeFormatter zoneAbbreviationFormatter
                    = DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH);
            offset = ZonedDateTime.now(zone).format(zoneAbbreviationFormatter);
            String postalcode = (String.valueOf((String) row.get("postalcode")));
            EntityModule e = new EntityModule(postalcode, offset);
            newEntity.add(e);
        }

        int sum = 0;
        Map<String, List<EntityModule>> finalData1 = newEntity.stream().collect(Collectors.groupingBy(EntityModule::getTimezoneApp));
        for (Map.Entry<String, List<EntityModule>> entry : finalData1.entrySet()) {
            List<String> postalNewCode = new ArrayList<>();
            for (int j = 0; j < entry.getValue().size(); j++) {
                postalNewCode.add(entry.getValue().get(j).getPostalCode());
            }
            String postalCodeNew = postalNewCode.toString().replace("[", "").replace("]", "");
            fileOverite(fileName, entry.getKey(), postalCodeNew);
            sum = sum + entry.getValue().size();

        }
        return "TotalSize" + sum;
    }

    private String CreateUpdateFile() {
        String fileName = "updateFile.txt";
        try {

            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                //System.out.println("File created: " + myObj.getName());
            } else {
                //System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return fileName;
    }

    @RequestMapping("/questAssessId")
    public String assessId() {


        String fileName = CreateUpdateFile();
        String query = "select concat(other_val,'@@',assess_id) from assess_other_data  aa\n" +
                "where \n" +
                "answer_val_id=604";


        List<String> availavleAssessId=new ArrayList<>();
        List<String> data = jdbcTemplate.queryForList(query, String.class);

        List<String> missingAssessId = new ArrayList<>();

        String assessId;
        HashMap<String,List<String>> newData=new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            String[] dataReplace = data.get(i).split("@@");

            assessId = dataReplace[1];

            List<String> sumOfString = new ArrayList<>();

            String[] arrOfStr = dataReplace[0].replace(".","").toString().split(",");

            for (String a : arrOfStr) {
                String finalQuery="select diagnosis_id from diagnoses   where LOWER(diagnosis_name) = concat(\"\",LOWER('"+a.trim().toLowerCase()+"'),\"\") limit 1";
              //  String findDiagonizId = "select  min(diagnosis_id) from diagnoses where lower(diagnosis_name) in('"++"')";


                System.out.println(""+finalQuery);

                    try {
                        Optional<String> findId = Optional.of(Optional.ofNullable(jdbcTemplate.queryForObject(finalQuery, String.class)).orElse(null));

                        availavleAssessId.add("" + a.trim() + "'");
                        sumOfString.add(findId.get());
                    }

                    catch(Exception e){
                   //     System.out.println("select  min(diagnosis_id) from diagnoses where diagnosis_name in('"+a.trim()+"')");

                        missingAssessId.add("'"+a.trim()+ "'");
                        sumOfString.add(a.trim().trim());
                    }

            }
            if(sumOfString.size()!=0)
            newData.put(assessId,sumOfString);


        }
        System.out.println("Missing disease\n\n "+missingAssessId);
      //  System.out.println(" Available Disease \n\n "+availavleAssessId);


        String updateQuery="";
        for (Map.Entry<String, List<String>> e : newData.entrySet()) {
            String k = e.getKey();
            fileOveriteNew(fileName,e.getKey(),e.getValue().toString().replace("[","").replace("]",""));
        }
        return ""+newData.size()+""+missingAssessId;


    }

    private void fileOveriteNew(String fileName, String key, String replace) {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));

            out.write("\nupdate assess_other_data set other_val_id= '" + replace + "'  where assess_id ="+ key + ";\n");
            // Closing the connection
            out.close();
        }

        // Catch block to handle the exceptions
        catch (IOException e) {

            // Display message when exception occurs
            System.out.println("exception occurred" + e);
        }
    }
}
