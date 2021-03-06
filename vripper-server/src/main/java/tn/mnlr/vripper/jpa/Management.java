package tn.mnlr.vripper.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableScheduling
public class Management {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern BACKUP_FILE_PATTERN = Pattern.compile("^db_(\\d{4}-\\d{2}-\\d{2})\\.tar\\.gz$");

    private final JdbcTemplate jdbcTemplate;
    private final String backupFolder;

    @Autowired
    public Management(JdbcTemplate jdbcTemplate, @Value("${base.dir}") String baseDir, @Value("${base.dir.name}") String baseDirName) {
        this.jdbcTemplate = jdbcTemplate;
        this.backupFolder = baseDir + File.separator + baseDirName + File.separator + "backup";
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 ? * *")
    private void backup() {

        for (File file : Optional.ofNullable(new File(backupFolder).listFiles()).orElse(new File[]{})) {
            Matcher matcher = BACKUP_FILE_PATTERN.matcher(file.getName());
            if (matcher.find()) {
                LocalDate localDate = LocalDate.parse(matcher.group(1), FORMATTER);
                if (localDate.isEqual(LocalDate.now())) {
                    return;
                }
            }
        }

        // create a backup file
        File backupFile = new File(backupFolder, String.format("db_%s.tar.gz", LocalDateTime.now().format(FORMATTER)));
        jdbcTemplate.execute(String.format("BACKUP DATABASE TO '%s' BLOCKING", backupFile.getPath()));
    }
}
