package org.carl.domain.qdrant;

import static org.carl.infrastructure.component.web.config.JSON.JSON;

import org.carl.domain.qdrant.filter.condition.Match;
import org.junit.jupiter.api.Test;

class MatchTest {
    @Test
    public void match() {
        String json =
"""
         {
                    "value": "Germany",
                    "any":["carl",1234],
                    "text":"match",
                    "except":["carl",1231]
         }

""";
        Match match = JSON.fromJsonX(json, Match.class);
        System.out.println(match);
    }
}
