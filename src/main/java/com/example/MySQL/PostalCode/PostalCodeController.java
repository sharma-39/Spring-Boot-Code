package com.example.MySQL.PostalCode;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.messaging.handler.annotation.Payload;
import com.example.MySQL.PostalCode.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.convert.out.html.HTMLExporterVisitor;
import org.docx4j.convert.out.html.HTMLConversionImageHandler;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.fit.pdfdom.PDFDomTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import javax.activation.DataHandler;
import javax.activation.DataSource;
//import javax.mail.Message;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.RequestBody;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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


    @Autowired
    PrintServiceImp printService;

    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");

    @Autowired
    JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public PostalCodeController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }


    @GetMapping(value = "/getData", produces = "application/hal+json")
    public ResponseEntity<Object> getDataFrom() {

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
    public String excelExprt() throws IOException {
        String filePath = "D:\\new.xlsx";

        String query = "select d.details_id,  date,particular,vc_type,vc_no,credit,d.column_1 as type,group_concat(dd.ref_no,'splited',dd.ref_type,'splited',dd.depit,'splited',dd.type) as detailsdata from details d \n" +
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


            List<Map<String, Object>> list = jdbcTemplate.queryForList(query);
            int previousId = 1;
            for (int i = 1; i < list.size(); i++) {

                int rowSize = previousId;
                Map row = list.get(i);
                Row dataRow = null;
                if (i == previousId) {
                    dataRow = sheet.createRow(rowSize);
                    String date = (String) row.get("date");
                    String particular = (String) row.get("particular");
                    String vc_type = (String) row.get("vc_type");
                    String vc_no = (String) row.get("vc_no");
                    String credit = (String) row.get("credit");
                    String type = (String) row.get("type");
                    dataRow.createCell(0).setCellValue(date);
                    dataRow.createCell(1).setCellValue(particular);
                    dataRow.createCell(2).setCellValue(vc_type);
                    dataRow.createCell(3).setCellValue(vc_no);
                    dataRow.createCell(4).setCellValue(credit);
                    dataRow.createCell(5).setCellValue(type);
                } else {
                    String detail_data = (String) row.get("detailsdata");
                    List<String> fieldAssocItem = Stream.of(detail_data.split(",")).map(String::trim)
                            .collect(Collectors.toList());
                    for (int m = 0; m < fieldAssocItem.size(); m++) {
                        dataRow = sheet.createRow(rowSize);
                        List<String> fieldAssocItem1 = Stream.of(detail_data.split("splited")).map(String::trim)
                                .collect(Collectors.toList());
                        int len = 6;
                        for (int n = 0; n < fieldAssocItem1.size(); n++) {
                            dataRow.createCell(len).setCellValue(fieldAssocItem1.get(n));
                            len++;
                        }
                        //   dataRow = sheet.createRow(rowSize);
                        previousId = fieldAssocItem.size();

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
    public String excelImport() throws IOException {

        List<String> filePathnew = new ArrayList<>();
        filePathnew.add("D:\\upload1.xlsx");
        filePathnew.add("D:\\upload.xls");

        for (int j = 0; j < filePathnew.size(); j++) {
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


        HashMap<Object, List<Object>> finalData = new HashMap<>();
        List<Object> data = new ArrayList<Object>();

        for (int i = 0; i < names.size(); i++) {
            Boolean nullPointerCheck = names.get(i).toString().equals("");
            Boolean nullPointerCheck2 = names.get(i).toString().equals(" ");
            Boolean nullPointerCheck3 = names.get(i).toString().trim().equals(" ");
            Boolean nullPointerCheck4 = names.get(i).toString().trim().equals("");
            Boolean nullPointerCheck5 = names.get(i).toString().trim().equals(" ,");
            Boolean nullPointerCheck6 = names.get(i).toString().trim().equals(", ");
            if (!(nullPointerCheck || nullPointerCheck2 || nullPointerCheck3 || nullPointerCheck4
                    || nullPointerCheck5 || nullPointerCheck6)
            ) {
                data.add(names.get(i).toString().trim());
            }
        }


        Map<Integer, List<Object>> hashMap = new HashMap<>();

        // System.out.println(""+);
        // Split the string into key-value pairs
        List<String> items = List.of(data.toString().split("externallink"));

        for (int i = 0; i < items.size(); i++) {

            List<Object> newData = Collections.singletonList(items.get(i));
            hashMap.put(i, newData);
        }
        //   System.out.println("finalData"+hashMap.);

        Map<Integer, List<Object>> hashMap2 = new HashMap<>();

        for (Map.Entry<Integer, List<Object>> entry : hashMap.entrySet()) {
            int key = entry.getKey();
            List<Object> value = new ArrayList<>(entry.getValue());
            // System.out.println("Original List " + key + ": " + value);
            // Modify the list
            value.add(value.size(), "finish");
            value.add(0, "start");
            hashMap2.put(key, value);
        }

        // Print the updated hashMap2
        System.out.println("\nUpdated hashMap2:");
        HashMap<Integer, List<String>> newFinalData = new HashMap<>();
        for (Map.Entry<Integer, List<Object>> entry : hashMap2.entrySet()) {
            int key = entry.getKey();
            List<Object> value = entry.getValue();
            List<Object> newData = new ArrayList<>();
            for (int i = 0; i < value.size(); i++) {
                //System.out.println(""+value.get(i));
                List<String> newData1 = new ArrayList<>();
                newData1.add("start");
                if (i == 1) {
                    String[] splitText = value.get(1).toString().split(",");
                    for (int j = 0; j < splitText.length; j++) {
                        //  System.out.println("\n\n"+splitText[j]);
                        splitText[j] = splitText[j].replace("]", "");
                        splitText[j] = splitText[j].replace(", ", "");

                        if (!splitText[j].isEmpty() || !(splitText[j].equals("]"))) {
                            newData1.add(splitText[j].trim());
                        }
                    }
                    newData1.removeIf(s -> s == null || s.isEmpty());
                    newData1.add("end");
                    newFinalData.put(key, newData1);
                }

            }
        }
        for (Map.Entry<Integer, List<String>> entry : newFinalData.entrySet()) {
            int key = entry.getKey();
            List<Object> value = new ArrayList<>(entry.getValue());

            System.out.println("Key" + key + " value " + value);
        }


        HashMap<List<String>, List<List<String>>> hashMapNew = new HashMap<>();
        System.out.println("" + newFinalData.size());
        for (int m = 1; m < newFinalData.size(); m++) {
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
                hashMapNew.put(sublist1, generateFinalHashMap(newData2));
            } catch (Exception e) {

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
            List<String> key = entry.getKey();
            List<List<String>> value = new ArrayList<>(entry.getValue());
            System.out.println("" + key);
            String urlnew = "INSERT INTO `newaccess`.`details` (date,vc_type, `particular`, `column_1`, `vc_no`, `credit`) VALUES" +
                    " ('" + key.get(1) + "', '" + key.get(2) + "', '" + key.get(3) + "', '" + key.get(4) + "','" + key.get(5) + "','" + key.get(6) + "');\n";
            KeyHolder holder2 = new GeneratedKeyHolder();

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(java.sql.Connection connection) throws SQLException {


                    PreparedStatement ps = connection.prepareStatement(urlnew,
                            Statement.RETURN_GENERATED_KEYS);

                    return ps;
                }

            }, holder2);

            for (int m = 0; m < value.size(); m++) {
                Integer id = holder2.getKey().intValue();

                String finalUrl = "INSERT INTO `newaccess`.`depit_bal` (`details_id`, `ref_type`, `ref_no`,`depit`, `type`) VALUES ('" + id + "', '" + value.get(m).get(0) + "', '" + value.get(m).get(1) + "', '" + value.get(m).get(2) + "', '" + value.get(m).get(3) + "')";

                jdbcTemplate.update(finalUrl);

            }


        }
    }

    private List<List<String>> generateFinalHashMap(List<String> newData2) {

        // System.out.println(""+newData2);
        String value = newData2.contains("Agst Ref") ? "Agst Ref" : "New Ref";


        List<List<String>> newList = new ArrayList<>();
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

    @RequestMapping("/send-whatsapp")
    public String timezoneConversion() throws IOException {


        // Find your Account SID and Auth Token at twilio.com/console
        // and set the environment variables. See http://twil.io/secure

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://graph.facebook.com/v18.0/545153065349016/messages"))
                    .header("Authorization", "Bearer EAAN40z0ZCChUBO4fNM5ZCa33ueN0NvqPNiqZAF7Y9PhecqDNrgiQG42xcgJgTPtYPlyUBYxBEQ6OezHepZAqJ4ZClSf21iRtZCtG9L31cvf8KGOy2k9gZCWt1vcnPSTPZBnxmtIjulT64dwSE6F2qOX7CPYxolwyMY7If8vH3ZAVPKaH7NqN709Gx2fxKWQr8ZCNTeh1aDxvroKQKrSZBOrbVr7WrWcm9ZAIZAKCpTm3178ew")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{"
                            + " \"messaging_product\": \"whatsapp\","
                            + " \"to\": \"919791310502\","
                            + " \"type\": \"text\","
                            + " \"text\": { \"body\": \"Hi\" }"
                            + " }"))
                    .build();

            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "success";
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

          //  mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse("sharmamurugaiyan@gmail.com"));

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
        List<String> data1 = Arrays.stream(idName.split("-", 2)).collect(Collectors.toUnmodifiableList());
        System.out.println("Data" + data1.get(0));
        String query = "select concat(id,'@',name) from clone_example where name like 'Condition%' ";
        List<String> clonedCount = jdbcTemplate.queryForList(query.replace("Condition", data1.get(0)), String.class);

        System.out.println("Size : " + clonedCount.size());
        System.out.println(name + " Copy " + (clonedCount.size() + 1));
        return null;
    }


    @GetMapping("uploadFileDocx")
    public String convertDocxtoHtml() {
        try {

            // Convert Base64 DOCX to Base64 HTML
            String filePath = "D:\\fileformat.txt";
            String base64Docx = new String(Files.readAllBytes(Paths.get(filePath)));
//            base64Docx ="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";
            String base64Html = convertBase64DocxToBase64Html(base64Docx);
            System.out.println("Base64 HTML: " + base64Html);
            return base64Html;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

    }

    @GetMapping("solution1")
    public String solution1() throws IOException {
        String filePath = "D:\\fileformat.txt";
        String base64Docx = new String(Files.readAllBytes(Paths.get(filePath)));

        // Decode Base64 to byte array
        byte[] docxBytes = Base64.getDecoder().decode(base64Docx);

        // Write byte array to a temporary file
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(docxBytes)) {
            // Load DOCX file into XWPFDocument
            XWPFDocument document = new XWPFDocument(inputStream);

            // Convert DOCX content to HTML
            Document htmlDoc = Jsoup.parse("<html><body></body></html>", "", Parser.xmlParser());
            Element body = htmlDoc.body();

            for (XWPFParagraph para : document.getParagraphs()) {
                body.appendElement("p").text(para.getText());
            }

            // Convert HTML output to Base64
            String html = htmlDoc.html();
            String base64HtmlOutput = Base64.getEncoder().encodeToString(html.getBytes());

            // Print or use the Base64-encoded HTML
            System.out.println(base64HtmlOutput);
            return base64HtmlOutput;
        }
        

    }

    public static String generateHtmlFromPdf(InputStream inputStream) throws IOException {
        PDDocument pdf = PDDocument.load(inputStream);
        PDFDomTree parser = new PDFDomTree();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer output = new PrintWriter(baos, true, StandardCharsets.UTF_8);
        parser.writeText(pdf, output);
        output.close();
        pdf.close();
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
    @PostMapping(value = "/convert-html")
    public ResponseEntity<String> convertToHtml(@RequestParam MultipartFile file) throws IOException {
        String html =generateHtmlFromPdf(file.getInputStream());
        return ResponseEntity.ok(html);
    }
    public String convertBase64DocxToBase64Html(String base64Docx) throws IOException {
        // Decode Base64 DOCX to byte array

        byte[] docxBytes = Base64.getDecoder().decode(base64Docx);

        // Convert DOCX byte array to HTML
        byte[] htmlData = convertPdfToHtml(docxBytes);

        // Encode HTML to Base64
        return Base64.getEncoder().encodeToString(htmlData);

    }

    private byte[] convertPdfToHtml(byte[] pdfData) throws IOException {
//        try (PDDocument document = PDDocument.load(pdfData)) {
//            StringBuilder htmlBuilder = new StringBuilder();
//            htmlBuilder.append("<html><head><style>")
//                    .append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }")
//                    .append("div { margin-bottom: 20px; }")
//                    .append("</style></head><body>");
//
//            PDPageTree pages = document.getPages();
//            for (PDPage page : pages) {
//                PDFTextStripper pdfStripper = new PDFTextStripper();
//                pdfStripper.setStartPage(document.getPages().indexOf(page) + 1);
//                pdfStripper.setEndPage(document.getPages().indexOf(page) + 1);
//                String text = pdfStripper.getText(document);
//                htmlBuilder.append("<div>")
//                        .append(text.replace("\n", "<br>"))
//                        .append("</div>");
//            }
//
//            htmlBuilder.append("</body></html>");
//            return htmlBuilder.toString().getBytes();
//        }
//        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
//            StringBuilder htmlBuilder = new StringBuilder();
//            htmlBuilder.append("<!DOCTYPE html><html lang='en'><head>")
//                    .append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
//                    .append("<title>Converted PDF</title>")
//                    .append("<style>body { font-family: Arial, sans-serif; margin: 20px; }</style>")
//                    .append("</head><body>");
//
//            PDPageTree pages = document.getPages();
//            for (org.apache.pdfbox.pdmodel.PDPage page : pages) {
//                PDFTextStripper pdfStripper = new PDFTextStripper();
//                pdfStripper.setStartPage(document.getPages().indexOf(page) + 1);
//                pdfStripper.setEndPage(document.getPages().indexOf(page) + 1);
//                String text = pdfStripper.getText(document);
//                htmlBuilder.append("<div style='page-break-before: always;'>")
//                        .append(text.replace("\n", "<br>"))
//                        .append("</div>");
//            }
//
//            htmlBuilder.append("</body></html>");
//            return htmlBuilder.toString().getBytes();
//        }

//        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
//            StringBuilder htmlBuilder = new StringBuilder();
//            htmlBuilder.append("<!DOCTYPE html><html lang='en'><head>")
//                    .append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
//                    .append("<title>Converted PDF</title>")
//                    .append("<style>")
//                    .append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }")
//                    .append(".page { page-break-before: always; margin: 20px; }")
//                    .append(".text { font-size: 12px; }") // Adjust font size as needed
//                    .append("</style>")
//                    .append("</head><body>");
//
//            PDPageTree pages = document.getPages();
//            for (PDPage page : pages) {
//                PDFTextStripper pdfStripper = new PDFTextStripper();
//                pdfStripper.setStartPage(document.getPages().indexOf(page) + 1);
//                pdfStripper.setEndPage(document.getPages().indexOf(page) + 1);
//                String text = pdfStripper.getText(document);
//                htmlBuilder.append("<div class='page'>")
//                        .append("<div class='text'>")
//                        .append(text.replace("\n", "<br>"))
//                        .append("</div>")
//                        .append("</div>");
//            }
//
//            htmlBuilder.append("</body></html>");
//            return htmlBuilder.toString().getBytes();
//        }

//        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
//            StringBuilder htmlBuilder = new StringBuilder();
//            htmlBuilder.append("<!DOCTYPE html><html lang='en'><head>")
//                    .append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
//                    .append("<title>Converted PDF</title>")
//                    .append("<style>")
//                    .append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }")
//                    .append(".page { page-break-before: always; margin: 20px; }")
//                    .append(".paragraph { text-align: justify; margin-bottom: 1em; }")
//                    .append("</style>")
//                    .append("</head><body>");
//
//            PDPageTree pages = document.getPages();
//            for (PDPage page : pages) {
//                PDFTextStripper pdfStripper = new PDFTextStripper();
//                pdfStripper.setStartPage(document.getPages().indexOf(page) + 1);
//                pdfStripper.setEndPage(document.getPages().indexOf(page) + 1);
//                String text = pdfStripper.getText(document);
//
//                // Process paragraphs manually
//                String[] paragraphs = text.split("\n\n");
//                for (String paragraph : paragraphs) {
//                    htmlBuilder.append("<p class='paragraph'>")
//                            .append(paragraph.replace("\n", "<br>"))
//                            .append("</p>");
//                }
//            }
//
//            htmlBuilder.append("</body></html>");
//            return htmlBuilder.toString().getBytes();
//        }

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);

            // Extract text and process it
            String text = pdfStripper.getText(document);

            // Convert text to HTML with basic formatting
            String htmlContent = "<html><head><style>"
                    + "body { font-family: Arial, sans-serif; margin: 20px; }"
                    + "p { margin: 0 0 1em; }"
                    + "h1, h2, h3, h4, h5, h6 { margin-top: 0; }"
                    + "</style></head><body>"
                    + processTextToHtml(text)
                    + "</body></html>";
            String base64Html = Base64.getEncoder().encodeToString(htmlContent.getBytes());
            return base64Html.getBytes();
        }
    }

    private String processTextToHtml(String text) {
        return text.replace("\n\n", "</p><p>").replace("\n", "<br/>");
    }


    private String convertDocxToHtml(byte[] docxBytes) throws IOException {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><style>");
        htmlContent.append("body { font-family: Arial, sans-serif; }");
        htmlContent.append("p { margin: 10px 0; }");
        htmlContent.append("h1, h2, h3 { margin-top: 20px; margin-bottom: 10px; }");
        htmlContent.append("</style></head><body>");

        try (ByteArrayInputStream bais = new ByteArrayInputStream(docxBytes);
             XWPFDocument document = new XWPFDocument(bais)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                htmlContent.append("<p style=\"");

                // Handling paragraph styles
                if (paragraph.getStyle() != null) {
                    htmlContent.append("font-size: ").append(paragraph.getStyle()).append("; ");
                }

                // Handling text runs
                for (XWPFRun run : paragraph.getRuns()) {
                    if (run.isBold()) {
                        htmlContent.append("font-weight: bold; ");
                    }
                    if (run.isItalic()) {
                        htmlContent.append("font-style: italic; ");
                    }
                    if (run.getUnderline() != UnderlinePatterns.NONE) {
                        htmlContent.append("text-decoration: underline; ");
                    }
                    htmlContent.append("font-size: ").append(run.getFontSize() != -1 ? run.getFontSize() + "px" : "inherit").append("; ");
                }

                htmlContent.append("\">");

                // Add text content
                htmlContent.append(paragraph.getText());

                htmlContent.append("</p>");
            }
        }

        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }


    @GetMapping(value = "/convert-html-solution", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> convertToHtmlsolution(@org.springframework.web.bind.annotation.RequestBody Base64File base64Pdf) throws IOException {
        // Decode Base64 PDF content


        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf.getBase64file());

        // Convert PDF to HTML
        String html = generateHtmlFromPdf(new ByteArrayInputStream(pdfBytes));

        // Encode HTML content to Base64
        String base64Html = Base64.getEncoder().encodeToString(html.getBytes());

        return ResponseEntity.ok(base64Html);
    }

    @GetMapping("conversion-doc-pdf")
    public String conversionProcessDocxtoPdf(@org.springframework.web.bind.annotation.RequestBody Base64File base64File)
    {


        try {
            // Step 1: Decode Base64 to byte array
            byte[] docxBytes = Base64.getDecoder().decode(base64File.getBase64file());

            // Step 2: Convert DOCX to PDF
            byte[] pdfBytes = printService.convertDocxToPdf(docxBytes);

            // Step 3: Encode PDF to Base64
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            System.out.println("Base64-encoded PDF: " + base64Pdf);
            return base64Pdf;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }



    @MessageMapping("/send")
    public void send(@Payload Message message) throws Exception {
        // Log the received message content
        System.out.println("Received: " + message.getContent());

        // Send the response to the topic
        messagingTemplate.convertAndSend("/topic/messages", new Message("Received: " + message.getContent()));
    }


    @GetMapping("/sendNotification")
    public String sendNotification() {
        Message message1 =new Message();
        messagingTemplate.convertAndSend("/topic/messages", new Message("Received:backend  New Notication\t\t"+new Date()  ));
        return "Notification sent: UI ";
    }






}
