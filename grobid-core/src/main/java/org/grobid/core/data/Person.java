package org.grobid.core.data;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class for representing and exchanging person information, e.g. author or editor.
 *
 */
public class Person {
    private String firstName = null;
    private String middleName = null;
    private String lastName = null;
    private String title = null;
    private String suffix = null;
    private String rawName = null; // raw full name if relevant/available, e.g. name exactly as displayed
    private String orcid = null;
    private boolean corresp = false;

    private List<LayoutToken> layoutTokens = new ArrayList<>();
    private List<String> affiliationBlocks = null;
    private List<Affiliation> affiliations = null;
    private List<String> affiliationMarkers = null;
    private List<String> markers = null;

    private String email = null;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String f) {
        firstName = f;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String f) {
        middleName = f;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String f) {
        lastName = f;
    }

    public String getRawName() {
         return rawName;
    }

    public void setRawName(String name) {
         rawName = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String f) {
        if (f != null) {
            while (f.startsWith("(")) {
                f = f.substring(1,f.length());
            }

            while (f.endsWith(")")) {
                f = f.substring(0,f.length()-1);
            }
        }

        title = f;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String s) {
        suffix = s;
    }

    public boolean getCorresp() {
        return corresp;
    }

    public void setCorresp(boolean b) {
        corresp = b;
    }

    public String getORCID() {
        return orcid;
    }

    public void setORCID(String id) {
        if (id == null)
            return;
        if (id.startsWith("http://orcid.org/"))
            id = id.replace("http://orcid.org/", "");
        else if (id.startsWith("https://orcid.org/"))
            id = id.replace("https://orcid.org/", "");
        orcid = id;
    }

    public List<String> getAffiliationBlocks() {
        return affiliationBlocks;
    }

    public void setAffiliationBlocks(List<String> blocks) {
        this.affiliationBlocks = blocks;
    }

    public void addAffiliationBlocks(String f) {
        if (affiliationBlocks == null)
            affiliationBlocks = new ArrayList<String>();
        affiliationBlocks.add(f);
    }

    public List<org.grobid.core.data.Affiliation> getAffiliations() {
        return affiliations;
    }

    public void addAffiliation(org.grobid.core.data.Affiliation f) {
        if (affiliations == null)
            affiliations = new ArrayList<>();
        affiliations.add(f);
    }

    public List<String> getAffiliationMarkers() {
        return affiliationMarkers;
    }

    public void setAffiliationMarkers(List<String> affiliationMarkers) {
        this.affiliationMarkers = affiliationMarkers;
    }

    public void addAffiliationMarker(String s) {
        if (affiliationMarkers == null)
            affiliationMarkers = new ArrayList<String>();
        affiliationMarkers.add(s);
    }

    public void setAffiliations(List<org.grobid.core.data.Affiliation> f) {
        affiliations = f;
    }

    public List<String> getMarkers() {
        return markers;
    }

    public void setMarkers(List<String> markers) {
        this.markers = markers;
    }

    public void addMarker(String f) {
        if (markers == null)
            markers = new ArrayList<String>();
		f = f.replace(" ", "");
        markers.add(f);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String f) {
        email = f;
    }

    public boolean notNull() {
        if ((firstName == null) &&
                (middleName == null) &&
                (lastName == null) &&
                (title == null)
                )
            return false;
        else
            return true;
    }

    /**
     * Create a new instance of Person object from current instance (shallow copy)
     */
    public Person clonePerson() {
        Person person = new Person();
        person.firstName = this.firstName ;
        person.middleName = this.middleName;
        person.lastName = this.lastName;
        person.title = this.title;
        person.suffix = this.suffix;
        person.rawName = this.rawName;
        person.orcid = this.orcid;
        person.corresp = this.corresp;
        person.email = this.email;

        if (this.layoutTokens != null)
            person.layoutTokens = new ArrayList<>(this.layoutTokens);
        if (this.affiliationBlocks != null)
            person.affiliationBlocks = new ArrayList<>(this.affiliationBlocks);
        if (this.affiliations != null)
            person.affiliations = new ArrayList<>(this.affiliations);
        if (this.affiliationMarkers != null)
            person.affiliationMarkers = new ArrayList<>(this.affiliationMarkers);
        if (this.markers != null)
            person.markers = new ArrayList<>(this.markers);

        return person;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (title != null) {
            sb.append(title).append(" ");
        }
        if (firstName != null) {
            sb.append(firstName).append(" ");
        }
        if (middleName != null) {
            sb.append(middleName).append(" ");
        }
        if (lastName != null) {
            sb.append(lastName).append(" ");
        }
        if (suffix != null) {
            sb.append(suffix);
        }
        if (email != null) {
            sb.append(" (email:").append(email).append(")");
        }
        if (orcid != null) {
            sb.append(" (orcid:").append(orcid).append(")");
        }
        if (affiliations != null) {
            for (Affiliation aff : affiliations) {
                sb.append(" (affiliation: ").append(aff.toString()).append(") ");
            }
        }
        return sb.toString().trim();
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> tokens) {
        this.layoutTokens = tokens;
    }

    /**
     * TEI serialization via xom.
     */
    public void appendLayoutTokens(List<LayoutToken> theTokens) {
        if (layoutTokens == null) {
            layoutTokens = new ArrayList<LayoutToken>();
        }
        layoutTokens.addAll(theTokens);
    }

    public String toTEI(boolean withCoordinates) {
        if ( (firstName == null) && (middleName == null) &&
                (lastName == null) ) {
            return null;
        }

        Element persElement = XmlBuilderUtils.teiElement("persName");

        if (withCoordinates && (getLayoutTokens() != null) && (!getLayoutTokens().isEmpty())) {
            XmlBuilderUtils.addCoords(persElement, LayoutTokensUtil.getCoordsString(getLayoutTokens()));
        }
        if (title != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("roleName", title));
        }
        if (firstName != null) {
            Element forename = XmlBuilderUtils.teiElement("forename", firstName);
            forename.addAttribute(new Attribute("type", "first"));
            persElement.appendChild(forename);
        }
        if (middleName != null) {
            Element mn = XmlBuilderUtils.teiElement("forename", middleName);
            mn.addAttribute(new Attribute("type", "middle"));
            persElement.appendChild(mn);
        }
        if (lastName != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("surname", lastName));
        }
        if (suffix != null) {
            persElement.appendChild(XmlBuilderUtils.teiElement("genName", suffix));
        }

        return XmlBuilderUtils.toXml(persElement);
    }

    /**
     * TEI serialization based on string builder, it allows to avoid namespaces and to better control
     * the formatting.
     */
    public String toTEI(boolean withCoordinates, int indent) {
        if ( (firstName == null) && (middleName == null) && (lastName == null) ) {
            return null;
        }

        StringBuilder tei = new StringBuilder();

        for (int i = 0; i < indent; i++) {
            tei.append("\t");
        }
        tei.append("<persName");
        if (withCoordinates && (getLayoutTokens() != null) && (!getLayoutTokens().isEmpty())) {
            tei.append(" ");
            tei.append(LayoutTokensUtil.getCoordsString(getLayoutTokens()));
        }
        tei.append(">\n");

        if (!StringUtils.isEmpty(title)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<roleName>"+TextUtilities.HTMLEncode(title)+"</roleName>\n");
        }

        if (!StringUtils.isEmpty(firstName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<forename type=\"first\">"+TextUtilities.HTMLEncode(firstName)+"</forename>\n");
        }

        if (!StringUtils.isEmpty(middleName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<forename type=\"middle\">"+TextUtilities.HTMLEncode(middleName)+"</forename>\n");
        }

        if (!StringUtils.isEmpty(lastName)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<surname>"+TextUtilities.HTMLEncode(lastName)+"</surname>\n");
        }

        if (!StringUtils.isEmpty(suffix)) {
            for (int i = 0; i < indent+1; i++) {
                tei.append("\t");
            }
            tei.append("<genName>"+TextUtilities.HTMLEncode(suffix)+"</genName>\n");
        }

        for (int i = 0; i < indent; i++) {
            tei.append("\t");
        }
        tei.append("</persName>");

        return tei.toString();
    }

    // list of character delimiters for capitalising names
 	private static final String NAME_DELIMITERS = "-.,;:/_ ";

    /*static public String normalizeName(String inputName) {
		return TextUtilities.capitalizeFully(inputName, NAME_DELIMITERS);
    }*/

    /**
     * This normalisation takes care of uniform case for name components and for
     * transforming agglutinated initials (like "JM" in JM Smith)
     * which are put into the firstname into separate initials in first and middle names.
     *
     */
    public void normalizeName() {
        if (StringUtils.isEmpty(middleName) && !StringUtils.isEmpty(firstName) &&
            (firstName.length() == 2) && (TextUtilities.isAllUpperCase(firstName)) ) {
            middleName = firstName.substring(1,2);
            firstName = firstName.substring(0,1);
        }

        firstName = TextUtilities.capitalizeFully(firstName, NAME_DELIMITERS);
        middleName = TextUtilities.capitalizeFully(middleName, NAME_DELIMITERS);
        lastName = TextUtilities.capitalizeFully(lastName, NAME_DELIMITERS);
    }

    // assume never more than 3 initials
    //private Pattern initials = Pattern.compile("([A-Z])(?:\\.)\\s?(?:([A-Z])(?:\\.))?\\s?(?:([A-Z])(?:\\.))?");

    /**
     * First names coming from CrossRef are clearly heavily impacted by the original puslisher
     * formats and a large variety of forms can be seen, with some information lost apparently.
     */
    public void normalizeCrossRefFirstName() {
        // first name can be initial with a dot, e.g. "M." or without a dot
        // <forename type="first">H</forename>

        // fistname can be intials with appended middlename also as initials,
        // with or without space, e.g. "M. L." or
        // <forename type="first">L.S.</forename>

        // normal full first name can be appended with middlename initials with dots but
        // no space e.g. "Nicholas J.", "John W.S."

        // we have sldo destructive case normalization done at CrossRef or by publishers
        // like "Zs. Biró"

        String first = null;
        String middle = null;

        /*Matcher m = initials.matcher(firstName);
        while(m.find()) {
            count++;
            System.out.println("Match number "+count);
            System.out.println("start(): "+m.start());
            System.out.println("end(): "+m.end());
            if (count != 0) {

            }
        }*/

        firstName = firstName.replace(".", ". ");
        firstName = StringUtils.normalizeSpace(firstName);

        // check first the specific case "Zs. Biró" - given the we've never observed three
        // letters first name like "Zsv. Biró"
        if ( firstName.endsWith(".") && (firstName.length() == 3) &&
            Character.isUpperCase(firstName.charAt(0)) && Character.isLowerCase(firstName.charAt(1)) ) {
            middleName = firstName.substring(1,2);
            firstName = firstName.substring(0,1);
        }

        // check the specific case of composed forenames which are often but not always lost
        // ex: "J.-L. Arsuag"
        if ( (firstName.indexOf("-") != -1) ) {
            String tokens[] = firstName.replace(" ", "").split("-");
            if (tokens.length == 2) {
                if (tokens[0].endsWith(".") && (tokens[0].length() == 2))
                    first = ""+tokens[0].charAt(0);
                else if (tokens[0].length() == 1)
                    first = tokens[0];
                if (tokens[1].endsWith(".") && (tokens[1].length() == 2))
                    first += "-" + tokens[1].charAt(0);
                else if (tokens[1].length() == 1)
                    first += "-" + tokens[1];
            }
        } else {
            String tokens[] = firstName.split(" ");
            for(int i=tokens.length-1; i>=0; i--) {
                if (i != 0) {
                    if (first != null) {
                        if (tokens[i].endsWith(".") && (tokens[i].length() == 2)) {
                            // (case "G. Arjen")
                            first = tokens[i].charAt(0) + " " + first;
                        } else {
                            // multiple token first name
                            first = tokens[i] + " " + first;
                        }
                    } else if ( (tokens[i].endsWith(".") && (tokens[i].length() == 2)) ||
                        (tokens[i].length() == 1) ) {
                        // we have an initials in secondary position, this is a middle name
                        if (middle == null)
                            middle = ""+tokens[i].charAt(0);
                        else
                           middle = tokens[i].charAt(0) + " " + middle;
                    } else {
                        if (middle == null)
                            middle = tokens[i];
                        else
                           middle = tokens[i] + " " + middle;
                    }
                } else {
                    // we check if we have an initial at the beginning (case "G. Arjen")
                    if (tokens[i].endsWith(".") && (tokens[i].length() == 2)) {
                        if (first == null)
                            first = ""+tokens[i].charAt(0);
                        else
                            first = tokens[i] + " " + first;
                    } else {
                        if (first == null)
                            first = tokens[i];
                        else
                            first = tokens[i] + " " + first;
                    }
                }
            }
        }

        if (first != null)
            firstName = first;
        if (middle != null)
            middleName = middle;

        // dirty case <forename type="first">HermanHG</forename><surname>Teerink</surname>
        if ( (firstName != null) && (middleName == null) && (firstName.length()>2) &&
             Character.isUpperCase(firstName.charAt(firstName.length()-1)) &&
             Character.isLowerCase(firstName.charAt(1)) ) {
            int i = firstName.length()-1;
            while(i>1) {
                if (Character.isUpperCase(firstName.charAt(i))) {
                    if (middleName == null)
                        middleName = ""+firstName.charAt(i);
                    else
                        middleName = firstName.charAt(i) + " " + middleName;
                } else
                    break;
                i--;
            }
            firstName = firstName.substring(0, i+1);
        }


        // for cases like JM Smith and for case normalisation
        normalizeName();

        // cleaning for CrossRef middlenames
        if (middleName != null) {
            middleName = middleName.replace(".", ". ");
            middleName = middleName.replace("  ", " ");
        }

        // other weird stuff: <forename type="first">G. Arjen</forename><surname>de Groot</surname>

        // also note that language specific case practice are usually not expected
        // e.g. H Von Allmen, J De
    }

	/**
	 *  Return true if the person structure is a valid person name, in our case
	 *  with at least a lastname or a raw name.
	 */
	public boolean isValid() {
		if ( (lastName == null) && (rawName == null) )
			return false;
		else
			return true;
	}


    /**
     *  Deduplicate person names, optionally attached to affiliations, based
     *  on common forename/surname, taking into account abbreviated forms
     */
    public static List<Person> deduplicate(List<Person> persons) {
        if (persons == null)
            return null;
        if (persons.size() == 0)
            return persons;

        // we create a signature per person based on lastname and first name first letter
        Map<String,List<Person>> signatures = new TreeMap<String,List<Person>>();

        for(Person person : persons) {
            if (person.getLastName() == null || person.getLastName().trim().length() == 0) {
                // the minimal information to deduplicate is not available
                continue;
            }
            String signature = person.getLastName().toLowerCase();
            if (person.getFirstName() != null && person.getFirstName().trim().length() != 0) {
                signature += "_" + person.getFirstName().substring(0,1);
            }
            List<Person> localPersons = signatures.get(signature);
            if (localPersons == null) {
                localPersons = new ArrayList<Person>();
            }
            localPersons.add(person);
            signatures.put(signature, localPersons);
        }

        // match signature and check possible affiliation information
        for (Map.Entry<String,List<Person>> entry : signatures.entrySet()) {
            List<Person> localPersons = entry.getValue();
            if (localPersons.size() > 1) {
                // candidate for deduplication, check full forenames and middlenames to check if there is a clash
                List<Person> newLocalPersons = new ArrayList<Person>();
                for(int j=0; j < localPersons.size(); j++) {
                    Person localPerson =  localPersons.get(j);
                    String localFirstName = localPerson.getFirstName();
                    if (localFirstName != null) {
                        localFirstName = localFirstName.toLowerCase();
                        localFirstName = localFirstName.replaceAll("[\\-\\.]", "");
                    }
                    String localMiddleName = localPerson.getMiddleName();
                    if (localMiddleName != null) {
                        localMiddleName = localMiddleName.toLowerCase();
                        localMiddleName = localMiddleName.replaceAll("[\\-\\.]", "");
                    }
                    int nbClash = 0;
                    for(int k=0; k < localPersons.size(); k++) {
                        boolean clash = false;
                        if (k == j)
                            continue;
                        Person otherPerson = localPersons.get(k);
                        String otherFirstName = otherPerson.getFirstName();
                        if (otherFirstName != null) {
                            otherFirstName = otherFirstName.toLowerCase();
                            otherFirstName = otherFirstName.replaceAll("[\\-\\.]", "");
                        }
                        String otherMiddleName = otherPerson.getMiddleName();
                        if (otherMiddleName != null) {
                            otherMiddleName = otherMiddleName.toLowerCase();
                            otherMiddleName = otherMiddleName.replaceAll("[\\-\\.]", "");
                        }

                        // test first name clash
                        if (localFirstName != null && otherFirstName != null) {
                            if (localFirstName.length() == 1 && otherFirstName.length() == 1) {
                                if (!localFirstName.equals(otherFirstName)) {
                                    clash = true;
                                }
                            } else {
                                if (!localFirstName.equals(otherFirstName) &&
                                    !localFirstName.startsWith(otherFirstName) &&
                                    !otherFirstName.startsWith(localFirstName)
                                    ) {
                                    clash = true;
                                }
                            }
                        }

                        // test middle name clash
                        if (!clash) {
                            if (localMiddleName != null && otherMiddleName != null) {
                                if (localMiddleName.length() == 1 && otherMiddleName.length() == 1) {
                                    if (!localMiddleName.equals(otherMiddleName)) {
                                        clash = true;
                                    }
                                } else {
                                    if (!localMiddleName.equals(otherMiddleName) &&
                                        !localMiddleName.startsWith(otherMiddleName) &&
                                        !otherMiddleName.startsWith(localMiddleName)
                                    ) {
                                    clash = true;
                                }
                                }
                            }
                        }

                        if (clash) {
                            // increase the clash number for index j
                            nbClash++;
                        }
                    }

                    if (nbClash == 0) {
                        newLocalPersons.add(localPerson);
                    }
                }

                localPersons = newLocalPersons;

                if (localPersons.size() > 1) {
                    // if identified duplication, keep the most complete person form and the most complete
                    // affiliation information
                    Person localPerson =  localPersons.get(0);
                    String localFirstName = localPerson.getFirstName();
                    if (localFirstName != null)
                        localFirstName = localFirstName.toLowerCase();
                    String localMiddleName = localPerson.getMiddleName();
                    if (localMiddleName != null)
                        localMiddleName = localMiddleName.toLowerCase();
                    String localTitle = localPerson.getTitle();
                    if (localTitle != null)
                        localTitle = localTitle.toLowerCase();
                    String localSuffix = localPerson.getSuffix();
                    if (localSuffix != null)
                        localSuffix = localSuffix.toLowerCase();
                    List<Affiliation> aff = localPerson.getAffiliations();
                    for (int i=1; i<localPersons.size(); i++) {
                        Person otherPerson = localPersons.get(i);
                        // try to enrich first Person object
                        String otherFirstName = otherPerson.getFirstName();
                        if (otherFirstName != null)
                            otherFirstName = otherFirstName.toLowerCase();
                        String otherMiddleName = otherPerson.getMiddleName();
                        if (otherMiddleName != null)
                            otherMiddleName = otherMiddleName.toLowerCase();
                        String otherTitle = otherPerson.getTitle();
                        if (otherTitle != null)
                            otherTitle = otherTitle.toLowerCase();
                        String otherSuffix = otherPerson.getSuffix();
                        if (otherSuffix != null)
                            otherSuffix = otherSuffix.toLowerCase();

                        if ((localFirstName == null && otherFirstName != null) ||
                            (localFirstName != null && otherFirstName != null &&
                            otherFirstName.length() > localFirstName.length())) {
                            localPerson.setFirstName(otherPerson.getFirstName());
                            localFirstName = localPerson.getFirstName().toLowerCase();
                        }

                        if ((localMiddleName == null && otherMiddleName != null) ||
                            (localMiddleName != null && otherMiddleName != null &&
                            otherMiddleName.length() > localMiddleName.length())) {
                            localPerson.setMiddleName(otherPerson.getMiddleName());
                            localMiddleName = localPerson.getMiddleName().toLowerCase();
                        }

                        if ((localTitle == null && otherTitle != null) ||
                            (localTitle != null && otherTitle != null &&
                            otherTitle.length() > localTitle.length())) {
                            localPerson.setTitle(otherPerson.getTitle());
                            localTitle = localPerson.getTitle().toLowerCase();
                        }

                        if ((localSuffix == null && otherSuffix != null) ||
                            (localSuffix != null && otherSuffix != null &&
                            otherSuffix.length() > localSuffix.length())) {
                            localPerson.setSuffix(otherPerson.getSuffix());
                            localSuffix = localPerson.getSuffix().toLowerCase();
                        }

                        String otherOrcid = otherPerson.getORCID();
                        if (otherOrcid != null)
                            localPerson.setORCID(otherOrcid);

                        if (otherPerson.getAffiliations() != null) {
                            for(Affiliation affOther : otherPerson.getAffiliations()) {
                                localPerson.addAffiliation(affOther);
                            }
                        }

                        if (otherPerson.getAffiliationBlocks() != null) {
                            for(String block : otherPerson.getAffiliationBlocks()) {
                                localPerson.addAffiliationBlocks(block);
                            }
                        }

                        if (otherPerson.getMarkers() != null) {
                            for(String marker : otherPerson.getMarkers()) {
                                if (localPerson.getMarkers() == null || !localPerson.getMarkers().contains(marker))
                                    localPerson.addMarker(marker);
                            }
                        }

                        if (localPerson.getEmail() == null)
                            localPerson.setEmail(otherPerson.getEmail());

                        if (persons.contains(otherPerson))
                            persons.remove(otherPerson);
                    }
                }
            }
        }

        return persons;
    }


    /**
     *  Remove invalid/impossible person names (no last names, noise, etc.)
     */
    public static List<Person> sanityCheck(List<Person> persons) {
        if (persons == null) {
            return null;
        }

        if (CollectionUtils.isEmpty(persons)) {
            return persons;
        }

        List<Person> result = new ArrayList<>();

        for(Person person : persons) {
            if (StringUtils.isNotBlank(person.getLastName())) {
                result.add(person);
            }
        }

        return result;
    }

}
