package com.miyo.weatherservice.components;

import com.miyo.weatherservice.services.WeatherService;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@EnableAsync
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    WeatherService weatherService;

    List<String> cities = Arrays.asList(/* CITIES */);

    //Method for local xml files every hour каждый час
    @Scheduled(cron = "0 * * * * *")
    @Async
    public void someLocalTask() {
        weatherService.documents.add(weatherService.getReportPerHour(cities));

        System.out.println("Local task performed");
        System.out.println(weatherService.documents);
    }

    @Scheduled(cron = "0 */3 * * * *") //срабатывает в час ночи каждый день
    @Async
    public void someGeneralTask() { //Method for sending final xml
        JSONObject general = new JSONObject();
        JSONObject daily = new JSONObject();
        for(JSONObject doc : weatherService.documents) {
            general.put("umk" + weatherService.documents.indexOf(doc), doc); //тут тоже вместо размера списка уникальный ключ-флаг
        }
        daily.put("daily", general); //Вместо ключа вставить дату
        weatherService.getAndSendXML(daily);
        System.out.println("General task performed");
        weatherService.documents.clear();
    }
}
