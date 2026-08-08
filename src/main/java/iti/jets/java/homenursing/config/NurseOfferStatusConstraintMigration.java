package iti.jets.java.homenursing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NurseOfferStatusConstraintMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NurseOfferStatusConstraintMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public NurseOfferStatusConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            var rows = jdbcTemplate.queryForList(
                    "SELECT pg_get_constraintdef(oid) AS def FROM pg_constraint WHERE conname = ?",
                    "nurse_offers_status_check");
            if (rows.isEmpty()) {
                return;
            }
            String constraintDef = String.valueOf(rows.get(0).get("def"));
            if (constraintDef.toUpperCase().contains("WITHDRAWN")) {
                return;
            }
            log.warn("Repairing nurse_offers_status_check: adding WITHDRAWN status");
            jdbcTemplate.execute("ALTER TABLE nurse_offers DROP CONSTRAINT nurse_offers_status_check");
            jdbcTemplate.execute("ALTER TABLE nurse_offers ADD CONSTRAINT nurse_offers_status_check "
                    + "CHECK (status IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN'))");
        } catch (Exception e) {
            log.error("Could not repair nurse_offers_status_check constraint", e);
        }
    }
}