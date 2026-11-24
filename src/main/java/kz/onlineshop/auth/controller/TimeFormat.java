package kz.onlineshop.auth.controller;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Шаблондарда уақытты Алматы уақытымен көрсету: ${@fmt.time(x)} */
@Component("fmt")
public class TimeFormat {

    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Almaty"));

    public String time(Instant instant) {
        return instant == null ? "—" : FORMAT.format(instant);
    }
}
