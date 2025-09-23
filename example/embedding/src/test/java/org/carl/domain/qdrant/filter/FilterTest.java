package org.carl.domain.qdrant.filter;

import static org.carl.infrastructure.component.web.config.JSON.JSON;

import org.carl.domain.qdrant.filter.condition.Condition;
import org.carl.domain.qdrant.filter.condition.DataTimeRange;
import org.carl.domain.qdrant.filter.condition.MatchKeyword;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class FilterTest {
    @Test
    void filter() {
        String json =
                """
                {
                  "must": [{
                  "matchKeyword":{"field":"name","keyword":"carl"},
                  "datetimeRange":{"field":"login","gt":null,"gte":"1747889635241","lt":null,"lte":null}
                  }]
                }
                """;
        Filter filter1 = new Filter();
        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setMatchKeyword(new MatchKeyword().setField("name").setKeyword("carl"));
        condition.setDatetimeRange(
                new DataTimeRange()
                        .setField("login")
                        .setGte(Timestamp.valueOf(LocalDateTime.parse("2020-01-01T00:00:00"))));
        conditions.add(condition);
        filter1.setMust(conditions);
        System.out.println(JSON.toJsonStringX(filter1));
        Filter filter = JSON.fromJsonX(json, Filter.class);
        System.out.println(filter);
    }

    @Test
    public void filterJson() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -30);
        System.out.println(now.getTimeInMillis());
    }
}
