package com.miyo.weatherservice.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class WeatherService {

    private static final String API_KEY = "API_KEY";

    private String content;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter docTime = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter hour =  DateTimeFormatter.ofPattern("mm"); //Поменять на часы

    public List<JSONObject> documents = new ArrayList<>();
    


    @Autowired
    private EmailAutoSender autoSender;

    public JSONObject getReportPerHour(List<String> cities) {
        JSONObject obj = new JSONObject();
        JSONObject report = new JSONObject();
        for(String city : cities)
            obj.put(city, getWeatherByCityName(city));
        report.put("weather-report-hour-" + hour.format(LocalDateTime.now()), obj); //вместо размера списка поставить какой-нибудь флаг
        return report;
    }

    List<String> cities = Arrays.asList("Moscow", "Tokyo", "Paris", "Strezhevoy");


    public void getAndSendXML(JSONObject obj) {

        String fileName = "src/main/resources/xml-files/report" + dateTimeFormatter.format(LocalDateTime.now()) + ".xml";

        String xml = XML.toString(obj);

        DOMSource source = null;
        try {
            source = new DOMSource(stringToDom(xml));
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StreamResult res = new StreamResult(writer);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        try {
            Objects.requireNonNull(transformer).transform(source, res);
        } catch (TransformerException e) {
            e.printStackTrace();
        }


        autoSender.sendEmail(fileName);

        try {
            Files.delete(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private JSONObject getWeatherByCityName(String cityName) {
        String inputLine;
        String urlStr = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&units=metric&lang=ru" + "&appid=" + API_KEY;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            content = response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject root = new JSONObject(content);
        JSONObject main = root.getJSONObject("main");
        JSONObject wind = root.getJSONObject("wind");
        JSONArray weather = root.getJSONArray("weather");
        JSONObject weas = weather.getJSONObject(0); //maybe i should check fixed index?

        JSONObject child = new JSONObject();

        child.put("temp", main.getDouble("temp"));
        child.put("humidity", main.getInt("humidity"));
        child.put("wind-speed", wind.getDouble("speed"));
        child.put("wind-direction", getTextualExpression(wind.getInt("deg")));
        child.put("illumination", weas.getString("description"));
        return child;

    }

    private String getTextualExpression(int degFromJSON) {
        double degree = degFromJSON;
        String[] sectors = {"Northerly","North Easterly","Easterly","South Easterly","Southerly","South Westerly","Westerly","North Westerly"};
        degree += 22.5;

        if (degree < 0)
            degree = 360 - Math.abs(degree) % 360;
        else
            degree = degree % 360;

        int which = (int) (degree / 45);
        return sectors[which];
    }

    public static Document stringToDom(String xmlSource)
            throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlSource)));
    }

}