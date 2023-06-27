package com.example.MySQL.PostalCode;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.RequestBody;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@RestController
public class PostalCodeController {

    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @GetMapping(value = "/getData", produces = "application/hal+json")
    public ResponseEntity<Object> getDataFrom()
    {

        String Query = "select  postal_id as postalId,Time_Zone_Abbr as appr,Time_Zone_Offset_from_api as offset,Postal_Code as postalCode  " +
                "from time_zone_data where Time_Zone_Offset_from_api is not null and Time_Zone_Offset_from_api!=\"\";";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(Query);
        ObjectMapper objectMapper = new ObjectMapper();
        Object json = null;
        try {
            json = objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // Handle the exception accordingly
        }

        return ResponseEntity.ok().body(json);
    }

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


    @RequestMapping("/excel-export")
    public  String   excelExprt() throws IOException {
        String filePath = "D:\\new.xlsx";

        String query="select d.details_id,  date,particular,vc_type,vc_no,credit,d.column_1 as type,group_concat(dd.ref_no,'splited',dd.ref_type,'splited',dd.depit,'splited',dd.type) as detailsdata from details d \n" +
                "join depit_bal dd on dd.details_id=d.details_id \n" +
                "where lower(particular) like lower(\"%KARUR%\")\n" +
                "group by d.details_id";





        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            // Write data to cells
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("date");
            headerRow.createCell(1).setCellValue("particular");
            headerRow.createCell(2).setCellValue("VcType");
            headerRow.createCell(3).setCellValue("VcNumber");
            headerRow.createCell(4).setCellValue("Credit");
            headerRow.createCell(5).setCellValue("Type");
            headerRow.createCell(6).setCellValue("details");


            List<Map<String, Object>> list=jdbcTemplate.queryForList(query);
            int previousId=1;
            for(int i=1;i<list.size();i++)
            {

                int rowSize=previousId;
                Map row=list.get(i);
                Row dataRow = null;
                if(i==previousId) {
                     dataRow = sheet.createRow(rowSize);
                    String date= (String) row.get("date");
                    String particular= (String) row.get("particular");
                    String vc_type= (String) row.get("vc_type");
                    String vc_no= (String) row.get("vc_no");
                    String credit= (String) row.get("credit");
                    String type= (String) row.get("type");
                    dataRow.createCell(0).setCellValue(date);
                    dataRow.createCell(1).setCellValue(particular);
                    dataRow.createCell(2).setCellValue(vc_type);
                    dataRow.createCell(3).setCellValue(vc_no);
                    dataRow.createCell(4).setCellValue(credit);
                    dataRow.createCell(5).setCellValue(type);
                }
                else
                {
                    String detail_data=(String) row.get("detailsdata");
                    List<String> fieldAssocItem = Stream.of(detail_data.split(",")).map(String::trim)
                            .collect(Collectors.toList());
                    for(int m=0;m<fieldAssocItem.size();m++)
                    {
                        dataRow = sheet.createRow(rowSize);
                        List<String> fieldAssocItem1 = Stream.of(detail_data.split("splited")).map(String::trim)
                                .collect(Collectors.toList());
                        int len = 6;
                        for(int n=0;n<fieldAssocItem1.size();n++)
                        {
                            dataRow.createCell(len).setCellValue(fieldAssocItem1.get(n));
                            len++;
                        }
                     //   dataRow = sheet.createRow(rowSize);
                        previousId=fieldAssocItem.size();

                    }
                }

            }

            // Save the workbook to a file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }

            System.out.println("Excel file created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "export successfully";
    }
        @RequestMapping("/excel-import")
    public  String   excelImport() throws IOException {

        List<String> filePathnew=new ArrayList<>();
        filePathnew.add("D:\\upload1.xlsx");
        filePathnew.add("D:\\upload.xls");

        for(int j=0;j<filePathnew.size();j++) {
            // String filePath = "D:\\upload.xls";
            String filePath = filePathnew.get(j);
            String sheetName = "Sheet1";
            String startCell = "A8";

            JsonSetReferenceNumber newRef;
            HashMap<List<String>, List<List<String>>> finalData = new HashMap<>();

            try {

                FileInputStream file = new FileInputStream(new File(filePath));
                Workbook workbook = new XSSFWorkbook(file);

                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet != null) {
                    CellReference startCellRef = new CellReference(startCell);
                    int startRow = startCellRef.getRow();
                    int startCol = startCellRef.getCol();
                    System.out.println("" + sheet.getLastRowNum());

                    List<Object> names = new ArrayList<Object>();
                    for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            for (int colIndex = startCol; colIndex < row.getLastCellNum(); colIndex++) {
                                Cell cell = row.getCell(colIndex);
                                if (cell != null) {
                                    String cellValue = "";
//                                switch (cell.getCellType()) {
//                                    case STRING:
//                                        cellValue = cell.getStringCellValue();
//                                        break;
//                                    case NUMERIC:
//                                        cellValue = String.valueOf(cell.getNumericCellValue());
//                                        break;
//                                    case BOOLEAN:
//                                        cellValue = String.valueOf(cell.getBooleanCellValue());
//                                        break;
//                                        // Add cases for other cell types as neede
//                                }
                                    boolean checkDateStart = false;
                                    boolean flag = false;
                                    if (cell.getCellType() == CellType.STRING) {
                                        cellValue = cell.getStringCellValue();
                                    } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                        Date date = cell.getDateCellValue();
                                        //   System.out.println("date "+date);

                                        SimpleDateFormat outputFormat = new SimpleDateFormat("d/MMMM/yyyy");
                                        String formattedDate = outputFormat.format(date);
                                        checkDateStart = true;
                                        cellValue = formattedDate;

                                    } else if (cell.getCellType() == CellType.NUMERIC) {
                                        cellValue = String.valueOf(cell.getNumericCellValue());
                                    }
                                    //   System.out.println("Cell Value: " + cellValue);

                                    if (checkDateStart != true) {
                                        checkDateStart = checkValidateDate(cellValue);
                                    }
                                    if (checkDateStart) {
                                        names.add("externallink");
                                        names.add(cellValue);

                                        flag = true;
                                    } else {
                                        names.add(cellValue);
                                    }

                                }
                            }
                        }
                    }
                    // displayData(names);
                    finalData = generatedDbData(names);

                }


                workbook.close();
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "Success Imported";
    }

    private HashMap<List<String>, List<List<String>>> generatedDbData(List<Object> names) {


        HashMap<Object,List<Object>> finalData=new HashMap<>();   List<Object> data=new ArrayList<Object>();

        for(int i=0;i<names.size(); i++) {
            Boolean nullPointerCheck=names.get(i).toString().equals("");
            Boolean nullPointerCheck2=names.get(i).toString().equals(" ");
            Boolean nullPointerCheck3=names.get(i).toString().trim().equals(" ");
            Boolean nullPointerCheck4=names.get(i).toString().trim().equals("");
            Boolean nullPointerCheck5=names.get(i).toString().trim().equals(" ,") ;
            Boolean nullPointerCheck6=names.get(i).toString().trim().equals(", ") ;
           if(!(nullPointerCheck || nullPointerCheck2 || nullPointerCheck3 || nullPointerCheck4
                   || nullPointerCheck5 || nullPointerCheck6)
           )
            {
                data.add(names.get(i).toString().trim());
            }
        }



        Map<Integer, List<Object>> hashMap = new HashMap<>();

       // System.out.println(""+);
        // Split the string into key-value pairs
        List<String> items= List.of(data.toString().split("externallink"));

        for(int i=0;i<items.size();i++)
        {

            List<Object> newData= Collections.singletonList(items.get(i));
            hashMap.put(i,newData);
        }
     //   System.out.println("finalData"+hashMap.);

        Map<Integer, List<Object>> hashMap2 = new HashMap<>();

        for (Map.Entry<Integer, List<Object>> entry : hashMap.entrySet()) {
            int key = entry.getKey();
            List<Object> value = new ArrayList<>(entry.getValue());
           // System.out.println("Original List " + key + ": " + value);
            // Modify the list
            value.add(value.size(),"finish");
            value.add(0,"start");
            hashMap2.put(key, value);
        }

        // Print the updated hashMap2
        System.out.println("\nUpdated hashMap2:");
        HashMap<Integer,List<String>> newFinalData=new HashMap<>();
        for (Map.Entry<Integer, List<Object>> entry : hashMap2.entrySet()) {
            int key = entry.getKey();
            List<Object> value = entry.getValue();
            List<Object> newData=new ArrayList<>();
            for(int i=0;i< value.size();i++)
            {
             //System.out.println(""+value.get(i));
             List<String> newData1=new ArrayList<>();
             newData1.add("start");
             if(i==1) {
                 String[] splitText = value.get(1).toString().split(",");
                 for (int j = 0; j < splitText.length; j++) {
                   //  System.out.println("\n\n"+splitText[j]);
                    splitText[j]= splitText[j].replace("]", "");
                     splitText[j]= splitText[j].replace(", ", "");

                     if(!splitText[j].isEmpty()  || !(splitText[j].equals("]"))) {
                         newData1.add(splitText[j].trim());
                     }
                 }
                 newData1.removeIf(s -> s == null || s.isEmpty());
                 newData1.add("end");
                 newFinalData.put(key,newData1);
             }

            }
        }
        for (Map.Entry<Integer, List<String>> entry : newFinalData.entrySet()) {
            int key = entry.getKey();
            List<Object> value = new ArrayList<>(entry.getValue());

            System.out.println("Key"+key+" value "+value);
        }


        HashMap<List<String>, List<List<String>>> hashMapNew = new HashMap<>();
        System.out.println(""+newFinalData.size());
        for(int m = 1;m<newFinalData.size(); m++) {
            List<List<String>> value = new ArrayList<>();
            List<String> newData2 = new ArrayList<>();
            List<String> newData3 = new ArrayList<>();
       //     System.out.println("" + newFinalData.get(m));
            List<String> sublist1 = newFinalData.get(m).subList(0, 7);

            if (newFinalData.get(m).contains("New Ref")) {
                boolean checkAlready = false;
                String startString = "New Ref";
                String endString = "end";
                int startIndex = newFinalData.get(m).toString().replace("[", "").replace("]", "").indexOf(startString) + startString.length();
                int endIndex = newFinalData.get(m).toString().replace("[", "").replace("]", "").indexOf(endString);
                String extractedString = newFinalData.get(m).toString().replace("[", "").replace("]", "").substring(startIndex, endIndex);

                //System.out.println(extractedString);
                String[] spinText = extractedString.split(",");
                newData2.add(startString);
                for (int l = 0; l < spinText.length; l++) {
                    newData2.add(spinText[l].trim());
                }
            }
            if (newFinalData.get(m).contains("Agst Ref")) {
                String startString = "Agst Ref";
                String endString = "end";

                newData2.add(startString);
                int startIndex = newFinalData.get(m).toString().replace("[", "").replace("]", "").indexOf(startString) + startString.length();
                int endIndex = newFinalData.get(m).toString().replace("[", "").replace("]", "").indexOf(endString);

                String extractedString = newFinalData.get(m).toString().replace("[", "").replace("]", "").substring(startIndex, endIndex);

                String[] spinText = extractedString.split(",");
                for (int l = 0; l < spinText.length; l++) {
                    newData2.add(spinText[l].trim());
                }

            }

            newData2.removeIf(s -> s == null || s.isEmpty());

            try {
                hashMapNew.put(sublist1,generateFinalHashMap(newData2));
            } catch (Exception e)
            {

            }



            // System.out.println(""+newData2);
          //  System.out.println(""+sublist1);

        }
        finalDbSaved(hashMapNew);
       System.out.println(hashMapNew.size());

        return hashMapNew;

    }

    private void finalDbSaved(HashMap<List<String>, List<List<String>>> hashMapNew) {

        System.out.println(hashMapNew.size());
        for (Map.Entry<List<String>, List<List<String>>> entry : hashMapNew.entrySet()) {
            List<String>  key = entry.getKey();
            List<List<String>> value = new ArrayList<>(entry.getValue());
            System.out.println(""+key);
            String urlnew="INSERT INTO `newaccess`.`details` (date,vc_type, `particular`, `column_1`, `vc_no`, `credit`) VALUES" +
                    " ('"+key.get(1)+"', '"+key.get(2)+"', '"+key.get(3)+"', '"+key.get(4)+"','"+key.get(5)+"','"+key.get(6)+"');\n";
            KeyHolder holder2 = new GeneratedKeyHolder();

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(java.sql.Connection connection) throws SQLException {


                    PreparedStatement ps = connection.prepareStatement(urlnew,
                            Statement.RETURN_GENERATED_KEYS);

                    return ps;
                }

            }, holder2);

            for(int m=0;m<value.size();m++)
            {
                Integer id= holder2.getKey().intValue();

                String finalUrl="INSERT INTO `newaccess`.`depit_bal` (`details_id`, `ref_type`, `ref_no`,`depit`, `type`) VALUES ('"+id+"', '"+value.get(m).get(0)+"', '"+value.get(m).get(1)+"', '"+value.get(m).get(2)+"', '"+value.get(m).get(3)+"')";

                jdbcTemplate.update(finalUrl);

            }



        }
    }

    private List<List<String>> generateFinalHashMap(List<String> newData2) {

       // System.out.println(""+newData2);
        String value=   newData2.contains("Agst Ref") ? "Agst Ref" : "New Ref";


        List<List<String>>  newList = new ArrayList<>();
        List<String> subList = new ArrayList<>();

        for (String item : newData2) {
            if (item.equals(value)) {
                if (!subList.isEmpty()) {
                    newList.add(subList);
                    subList = new ArrayList<>();
                }
            }
            subList.add(item);
        }
        newList.add(subList);

        // Print the new list
        for (List<String> list : newList) {
         //   System.out.println(list);
        }
     return newList;

    }



    private void displayData(List<Object> names) {
        System.out.println("" + names);
    }

    private Boolean checkValidateDate(String cellValue) {
        String inputDate = cellValue;
        SimpleDateFormat inputFormat = new SimpleDateFormat("d/MMMM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        boolean flag = false;
       // System.out.println("" + cellValue);
        try {
            Date date = inputFormat.parse(inputDate);
            String formattedDate = outputFormat.format(date);
            flag = true;
        } catch (ParseException e) {

            // System.out.println("else part");
            //e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    @RequestMapping("/timezoneConversion")
    public String timezoneConversion() throws IOException {


        // Find your Account SID and Auth Token at twilio.com/console
        // and set the environment variables. See http://twil.io/secure

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://graph.facebook.com/v13.0/107634205625641/messages"))
                    .header("Authorization", "Bearer EAAIWg4W2rjQBAHRbt6EWplA9K8GqLAYmk9b9eAS3FkZAZCKiitQhUyU8YKFMySFOp1ex4crEzj8MZBElg5bgdfGpBOUQOaFXvZBunrNwNF4oXspEZBrOgkKgDZBzO0siohgGJ6oOZBN4bv03VBuZCYLyJorN5FgjXWUofHAntlsEVeYMATaiA3sKjPeDlfMBdgCfkiKVq2qfYwZDZD")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"messaging_product\": \"whatsapp\", \"recipient_type\": \"individual\", \"to\": \"919791310502\", \"type\": \"template\", \"template\": { \"name\": \"Hi Sharma Murugaiyan\", \"language\": { \"code\": \"en_US\" } } }"))
                    .build();
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        OkHttpClient client = new OkHttpClient();
        okhttp3.RequestBody body = new FormBody.Builder()
                .add("token", "o7d415viacyz5q6q")
                .add("to", "9791310502")
                .add("body", "WhatsApp API on UltraMsg.com works good")
                .build();

        Request request = new Request.Builder()
                .url("https://api.ultramsg.com/instance41905/messages/chat")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withLocale(Locale.ENGLISH);

        LocalDate date = LocalDate.parse("29-02-2019", formatter);

        String plusDate = date.plusDays(7).toString();

        System.out.println("---" + plusDate);
        String format = generateTimeZoneOffset("America/New_York");
        return "" + UtcToCgTimeZoneConversion("America/New_York", "2023-02-15 07:58:21", true);
    }

    public String UtcToCgTimeZoneConversion(String zone, String date, boolean sigDateFormat) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM,dd yyyy HH:mm a")
                .withZone(ZoneId.of(zone));
        Instant instant;
        if (date.equalsIgnoreCase("now()")) {
            formatter = DateTimeFormatter.ofPattern("MMM,dd yyyy HH:mm:ss a")
                    .withZone(ZoneId.of(zone));
            instant = Instant.now();
        } else if (sigDateFormat) {
            formatter = DateTimeFormatter.ofPattern("MMM,dd yyyy HH:mm a")
                    .withZone(ZoneId.of(zone));
            instant = Timestamp.valueOf(date).toInstant();
        } else {
            instant = Timestamp.valueOf(date).toInstant();
        }
        String formattedInstant = formatter.format(instant);
        return formattedInstant;
    }

    private String generateTimeZoneOffset(String zoneOffset) {
        String offset = ZoneId.of(zoneOffset).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH);
        DateTimeFormatter zoneAbbreviationFormatter
                = DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH);
        String offsetLabel = ZonedDateTime.now(ZoneId.of(zoneOffset)).format(zoneAbbreviationFormatter);
        return offsetLabel;
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


        List<String> availavleAssessId = new ArrayList<>();
        List<String> data = jdbcTemplate.queryForList(query, String.class);

        List<String> missingAssessId = new ArrayList<>();

        String assessId;
        HashMap<String, List<String>> newData = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            String[] dataReplace = data.get(i).split("@@");

            assessId = dataReplace[1];

            List<String> sumOfString = new ArrayList<>();

            String[] arrOfStr = dataReplace[0].replace(".", "").toString().split(",");

            for (String a : arrOfStr) {
                String finalQuery = "select diagnosis_id from diagnoses   where LOWER(diagnosis_name) = concat(\"\",LOWER('" + a.trim().toLowerCase() + "'),\"\") limit 1";
                //  String findDiagonizId = "select  min(diagnosis_id) from diagnoses where lower(diagnosis_name) in('"++"')";


                System.out.println("" + finalQuery);

                try {
                    Optional<String> findId = Optional.of(Optional.ofNullable(jdbcTemplate.queryForObject(finalQuery, String.class)).orElse(null));

                    availavleAssessId.add("" + a.trim() + "'");
                    sumOfString.add(findId.get());
                } catch (Exception e) {
                    //     System.out.println("select  min(diagnosis_id) from diagnoses where diagnosis_name in('"+a.trim()+"')");

                    missingAssessId.add("'" + a.trim() + "'");
                    sumOfString.add(a.trim().trim());
                }

            }
            if (sumOfString.size() != 0)
                newData.put(assessId, sumOfString);


        }
        System.out.println("Missing disease\n\n " + missingAssessId);
        //  System.out.println(" Available Disease \n\n "+availavleAssessId);


        String updateQuery = "";
        for (Map.Entry<String, List<String>> e : newData.entrySet()) {
            String k = e.getKey();
            fileOveriteNew(fileName, e.getKey(), e.getValue().toString().replace("[", "").replace("]", ""));
        }
        return "" + newData.size() + "" + missingAssessId;


    }

    private void fileOveriteNew(String fileName, String key, String replace) {
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));

            out.write("\nupdate assess_other_data set other_val_id= '" + replace + "'  where assess_id =" + key + ";\n");
            // Closing the connection
            out.close();
        }

        // Catch block to handle the exceptions
        catch (IOException e) {

            // Display message when exception occurs
            System.out.println("exception occurred" + e);
        }
    }

    @PostMapping("/mail-form")
    public String formMail() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");


            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("sharmamurugaiyan48@gmail.com", "prfhldyosmnfehjt");
                }
            });
            // System.out.println(""+session.getPasswordAuthentication());

            MimeBodyPart textBodyPart = new MimeBodyPart();

            String html = "   " +
//                " <html>" +
//                "\n" +
//                "      <body>" +
//                "        <form  action=\"http://localhost:8080/redirect\" method=\"POST\">\n" +
//                "          <input type=\"text\" name=\"fullname\" value=\"Sam\" />\n" +
//                "          <input type=\"text\" name=\"city\" value=\"Dubai&#32;\" />\n" +
//                "          <input onclick=\"window.location.href = 'https://website.com/my-account';\" type=\"submit\" value=\"Submit request\" />\n" +
//                "        </form>\n" +
//                "      </body>\n" +
//                "    </html>";
//                "<!DOCTYPE html>\n" +
//
//                "<html>\n" +
//                "<head>\n" +
//                "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.6.4/jquery.min.js\"></script>\n" +
//                "<script>\n" +
//                "$(document).ready(function(){\n" +
//                "  $(\"button\").click(function(){\n" +
//                "    $.post(\"http://localhost:8080/redirect\",\n" +
//                "    {\n" +
//                "      name: \"Donald Duck\",\n" +
//                "      city: \"Duckburg\"\n" +
//                "    },\n" +
//                "    function(data,status){\n" +
//                "      alert(\"Data: \" + data + \"\\nStatus: \" + status);\n" +
//                "    });\n" +
//                "  });\n" +
//                "});\n" +
//                "</script>\n" +
//                "</head>\n" +
//                "<body>\n" +
//                "\n <form method='post'>" +
//                "<button type='submit'>Send an HTTP POST request to a page and get the result back</button>\n" +
//                "\n</form>" +
//                "</body>\n" +
//                "</html>\n";
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "<meta charset=\"utf-8\">\n" +
                    "<title>jQuery AJAX Submit Form</title>\n" +
                    "<script src=\"https://code.jquery.com/jquery-3.5.1.min.js\"></script>\n" +
                    "<script>\n" +
                    "$(document).ready(function(){\n" +
                    "    $(\"form\").on(\"submit\", function(event){\n" +
                    "       ;\n" +
                    " \n" +
                    "        var formValues= $(this).serialize();\n" +
                    "        var actionUrl = $(this).attr(\"action\");\n" +
                    " \n" +
                    "        $.post(actionUrl, formValues, function(data){\n" +
                    "            // Display the returned data in browser\n" +
                    "           alert(\"Success\")\n" +
                    "        });\n" +
                    "    });\n" +
                    "});\n" +
                    "</script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <form action=\"http://localhost:8080/redirect\" method='POST'>\n" +
                    "        <p>\n" +
                    "            <label>Name:</label>\n" +
                    "            <input type=\"text\" name=\"name\">\n" +
                    "        </p>\n" +
                    "        <p>\n" +
                    "            <label>Gender:</label>\n" +
                    "            <label><input type=\"radio\" value=\"male\" name=\"gender\"> Male</label>\n" +
                    "            <label><input type=\"radio\" value=\"female\" name=\"gender\"> Female</label>\n" +
                    "        </p>\n" +
                    "        <p>\n" +
                    "        <p>\n" +
                    "            <label>Favorite Color:</label>\n" +
                    "            <select name=\"color\">\n" +
                    "                <option>Red</option>\n" +
                    "                <option>Green</option>\n" +
                    "                <option>Blue</option>\n" +
                    "            </select>\n" +
                    "        </p>\n" +
                    "        <p>\n" +
                    "            <label>Comment:</label>\n" +
                    "            <textarea name=\"comment\"></textarea>\n" +
                    "        </p>\n" +
                    "        <input type=\"submit\" value=\"submit\">\n" +
                    "    </form>\n" +
                    "    <div id=\"result\"></div>\n" +
                    "</body>\n" +
                    "</html>";
            //   "<html>\n" +
//                "\n" +
//                "<head>\n" +
//                "<title>Simple JQuery Post Form to PHP Example</title>\n" +
//                "</head>\n" +
//                "\n" +
//                "<body>\n" +
//                "\n" +
//                "<!-- including jQuery from the google cdn -->\n" +
//                "<script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.5.1/jquery.min.js\">      </script>\n" +
//                "\n" +
//                "<!-- This div will be populated with error messages -->\n" +
//                "<div id=\"example_form_error\"></div>\n" +
//                "\n" +
//                "<!-- This div will be populated with success messages -->\n" +
//                "<div id=\"example_form_success\"></div>\n" +
//                "\n" +
//                "<!-- Here is your form -->\n" +
//                "<div id=\"example_form_enter\">\n" +
//                "    <form id=\"contact_modal_form\" method='post' action='http://localhost:8080/redirect'>\n" +
//                "            <label for=\"Name\">Enter Your Name (Not \"Adam\"):</label> <input class='textbox' name='Name' type='text' size='25' required />\n" +
//                "            <button class='contact_modal_button' type='submit'>Send</button>\n" +
//                "    </form>\n" +
//                "</div>\n" +
//                "\n" +
//                "<!-- This div contains a section that is hidden initially, but displayed when the form is submitted successfully -->\n" +
//                "<div id=\"example_form_confirmation\" style=\"display: none\">\n" +
//                "    <p>\n" +
//                "        Additional static div displayed on success.\n" +
//                "        <br>\n" +
//                "        <br>\n" +
//                "        <a href=\"form.php\">Try Again</a>\n" +
//                "    </p>\n" +
//                "</div>\n" +
//                "\n" +
//                "<!-- Below is the jQuery function that process form submission and receives back results -->\n" +
//                "<script>\n" +
//                "    $(function() {\n" +
//                "        $(\"#contact_modal_form\").submit(function(event) {\n" +
//                "            var form = $(this);\n" +
//                "            $.ajax({\n" +
//                "                type: form.attr('method'),\n" +
//                "                url: form.attr('action'),\n" +
//                "                data: form.serialize(),\n" +
//                "                dataType: 'json',\n" +
//                "                success: function(data) {\n" +
//                "                    if(data.error == true) {\n" +
//                "                        var error = $(\"#example_form_error\");\n" +
//                "                        error.css(\"color\", \"red\");\n" +
//                "                        error.html(\"Not \" + data.msg + \". Please enter a different name.\");\n" +
//                "                    } else {\n" +
//                "                        $(\"#example_form_enter\").hide();\n" +
//                "                        $(\"#example_form_error\").hide();\n" +
//                "                        $(\"#example_form_confirmation\").show();\n" +
//                "\n" +
//                "                        var success = $(\"#example_form_success\");\n" +
//                "                        success.css(\"color\", \"green\");\n" +
//                "                        success.html(\"Success! You submitted the name \" + data.msg + \".\");\n" +
//                "                    }\n" +
//                "                }\n" +
//                "            });\n" +
//                "            event.preventDefault();\n" +
//                "        });\n" +
//                "    });\n" +
//                "</script>\n" +
//                "\n" +
//                "</body>\n" +
//                "\n" +
//                "</html>";
//                "<!-- HTML form with an ID \"myForm\" -->\n" +
//                "<form id=\"myForm\" action=\"http://localhost:8080/redirect\" method=\"post\">\n" +
//                "  <input type=\"text\" name=\"name\" placeholder=\"Name\">\n" +
//                "  <input type=\"email\" name=\"email\" placeholder=\"Email\">\n" +
//                "  <button type=\"submit\">Submit</button>\n" +
//                "</form>\n" +
//                "\n" +
//                "<!-- HTML section to display the response from the server -->\n" +
//                "<div id=\"response\"></div>\n" +
//                "\n" +
//                "<!-- JavaScript code to handle the form submission -->\n" +
//                "<script>\n" +
//                "  // Get the form element\n" +
//                "  var form = document.getElementById(\"myForm\");\n" +
//                "\n" +
//                "  // Attach an event listener to the form submit event\n" +
//                "  form.addEventListener(\"submit\", function(event) {\n" +
//                "    // Prevent the default form submission behavior\n" +
//                "    event.preventDefault();\n" +
//                "\n" +
//                "    // Create a new XMLHttpRequest object\n" +
//                "    var xhttp = new XMLHttpRequest();\n" +
//                "\n" +
//                "    // Define the function to be executed when the response is received\n" +
//                "    xhttp.onreadystatechange = function() {\n" +
//                "      if (this.readyState == 4 && this.status == 200) {\n" +
//                "        // Code to be executed when the response is received\n" +
//                "        document.getElementById(\"response\").innerHTML = this.responseText;\n" +
//                "      }\n" +
//                "    };\n" +
//                "\n" +
//                "    // Open the HTTP request and set the request method to POST\n" +
//                "    xhttp.open(\"POST\", \"http://localhost:8080/redirect\", true);\n" +
//                "\n" +
//                "    // Set the request header to indicate that the form data is being sent\n" +
//                "    xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n" +
//                "\n" +
//                "    // Get the form data and send the HTTP request\n" +
//                "    xhttp.send(new FormData(form));\n" +
//                "  });\n" +
//                "</script>\n";
//                "<!DOCTYPE html>\n" +
//                "<html>\n" +
//                "\n" +
//                "<head>\n" +
//                "\t<title>Form Submittion without using submit Button</title>\n" +
//                "</head>\n" +
//                "\n" +
//                "<body>\n" +
//                "\t<h1 style=\"color: green\">\n" +
//                "\t\tGeeksforGeeks\n" +
//                "\t</h1>\n" +
//                "\t<h3>\n" +
//                "\t\tForm Submittion without using submit Button\n" +
//                "\t</h3>\n" +
//                "\t<form id=\"form\"\n" +
//                "\t\taction=\"http://localhost:8080/redirect\"\n" +
//                "\t\tmethod=\"post\">\n" +
//                "\t\t<label>\n" +
//                "\t\t\tSubscribe the GBlog to get daily update!!!\n" +
//                "\t\t</label>\n" +
//                "\t\t<br><br>\n" +
//                "\t\t<input type=\"text\"\n" +
//                "\t\t\tname=\"text\"\n" +
//                "\t\t\tplaceholder=\"Enter Mail ID\" />\n" +
//                "\t\t<br/>\n" +
//                "\t\t<h5 onclick=\"submit()\">\n" +
//                "\t\t\tClick Me to Subscribe!\n" +
//                "\t\t</h5>\n" +
//                "\t</form>\n" +
//                "\t<script>\n" +
//                "\t\tfunction submit() {\n" +
//                "\t\t\tlet form = document.getElementById(\"form\");\n" +
//                "\t\t\t\n" +
//                "\t\t\talert(\"Data stored in database!\");\n" +
//                "\t\t}\n" +
//                "\t</script>\n" +
//                "</body>\n" +
//                "</html>\n";
//                 "<html><body><h1>Hello!</h1><p>This is an example email with a button.</p>"
//                + "<form action=\"http://localhost:8080/redirect\" method=\"POST\">"
//                + "<input type=\"hidden\" name=\"action\" value=\"submit\">"
//                + "<button type=\"submit\" onclick=\"event.preventDefault(); this.disabled=true; this.form.submit();\">Click me</button>"
//                + "</form></body></html>";
            System.out.println("" + html);

            String content = html;
            byte[] bytesContent = content.getBytes();


            DataSource dataSourceHtml = new ByteArrayDataSource(bytesContent, "text/html");
            textBodyPart.setDataHandler(new DataHandler(dataSourceHtml));

            MimeMultipart mimeMultipart = new MimeMultipart();
            mimeMultipart.addBodyPart(textBodyPart);

            InternetAddress iaSender = new InternetAddress("sharmamurugaiyan48@gmail.com");

            // construct the mime message
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSender(iaSender);
            mimeMessage.setSubject("Will check filled form");

            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse("sharmamurugaiyan@gmail.com"));

            mimeMessage.setFrom(new InternetAddress("sharmamurugaiyan48@gmail.com"));
            mimeMessage.setReplyTo(InternetAddress.parse("sharmamurugaiyan48@gmail.com", false));
            mimeMessage.setContent(mimeMultipart);

            // send off the email
            Transport.send(mimeMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Success";
    }

    @PostMapping(value = "/redirect")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<String> redirect(@RequestParam Map<String, String> input) {

        System.out.println(input);

        return new ResponseEntity<String>("It's working...!", HttpStatus.OK);
    }

    //    @PostMapping(value = "/redirect")
//    @CrossOrigin(origins = "*", allowedHeaders = "*")
//    public String getData() {
//        Map<String, Object> data = new HashMap<>();
//        data.put("message", "Hello, world!");
//        System.out.println(""+data);
//        return "hello";
//    }
    @RequestMapping("/clone")
    public String cloneId(@RequestParam String name, @RequestParam String id) {
        String copyNameDb = jdbcTemplate.queryForObject("select concat(id,',',name) from clone_example where id=?", String.class, id);

        String[] cloneName = copyNameDb.split(",");
        String idDb = cloneName[0];
        String idName = cloneName[1];
//lambda Experission
//      String delimiter="-";
//      Pattern pattern = Pattern.compile(delimiter);
//      splitString.forEach(x-> System.out.print(pattern.splitAsStream(x).collect(Collectors.toList()).get(0))) ;
        List<String> data1 = Arrays.stream(idName.split("-", 2)).toList();
        System.out.println("Data" + data1.get(0));
        String query = "select concat(id,'@',name) from clone_example where name like 'Condition%' ";
        List<String> clonedCount = jdbcTemplate.queryForList(query.replace("Condition", data1.get(0)), String.class);

        System.out.println("Size : " + clonedCount.size());
        System.out.println(name + " Copy " + (clonedCount.size() + 1));
        return null;
    }


}
