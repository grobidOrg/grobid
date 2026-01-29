package org.grobid.core.data;

import org.grobid.core.data.util.AuthorEmailAssigner;
import org.grobid.core.data.util.ClassicAuthorEmailAssigner;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AuthorEmailAssignerTest {

    @Test
    public void testEmailAssignment() {
        AuthorEmailAssigner assigner = new ClassicAuthorEmailAssigner();

        List<Person> authors = l(
                p("Jalal Al-Muhtadi"),
                p("Manish Anand"),
                p("M.", "Dennis Mickunas"),
                p("Roy Campbell"));
        assigner.assign(authors, l("almuhtad@uiuc.edu", "manand@uiuc.edu", "mickunas@uiuc.edu", "rhc@uiuc.edu"));

        System.out.println(authors);

    }

    private Person p(String name) {
        String[] split = name.split(" ");
        return p(split[0], split[1]);
    }

    private Person p(String fistName, String lastName) {
        Person p = new Person();
        p.setFirstName(fistName.trim());
        p.setLastName(lastName.trim());
        return p;
    }

    public static <T> List<T> l(T... els) {
        return Arrays.asList(els);
    }

}
