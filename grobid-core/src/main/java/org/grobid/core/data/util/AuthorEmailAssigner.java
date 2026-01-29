package org.grobid.core.data.util;

import org.grobid.core.data.Person;

import java.util.List;

public interface AuthorEmailAssigner {
    // embeds emails into authors
    // emails should be sanitized before
    public void assign(List<Person> authors, List<String> emails);
}
