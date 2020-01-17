package eu.devexpert.rules.facts

import org.jeasy.rules.annotation.Action
import org.jeasy.rules.annotation.Condition
import org.jeasy.rules.annotation.Fact
import org.jeasy.rules.annotation.Rule
import org.jeasy.rules.api.Facts;

@Rule(name = "my rule", description = "my rule description", priority = 1)
class LightOn {
    @Condition
    def when(@Fact("fact") fact) {
        return true;
    }

    @Action(order = 1)
    def then(Facts facts) throws Exception {
        //my actions
    }

    @Action(order = 2)
    def second() throws Exception {
        //my final actions
    }


}
