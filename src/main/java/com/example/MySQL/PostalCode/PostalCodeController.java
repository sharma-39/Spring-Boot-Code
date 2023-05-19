package com.example.MySQL.PostalCode;


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
import java.util.*;

import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.stream.StreamSupport;


@RestController
public class PostalCodeController {

    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
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
    public String formMail()

    {try {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");


        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("sharmamurugaiyan48@gmail.com","prfhldyosmnfehjt");
            }
        });
       // System.out.println(""+session.getPasswordAuthentication());

        MimeBodyPart textBodyPart = new MimeBodyPart();

        String html ="   " +
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
                System.out.println(""+html);

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

    }catch(Exception e) {
        e.printStackTrace();
    }
        return "Success";
    }

    @PostMapping(value = "/redirect")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<String> redirect(@RequestParam Map<String,String> input){

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
