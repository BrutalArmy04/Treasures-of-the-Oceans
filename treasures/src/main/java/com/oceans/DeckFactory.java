package com.oceans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the master deck from {@code fish_seed.csv} on the classpath.
 *
 * The species list used to be sixty hand-written {@code new Card(...)} lines here, which
 * meant adding a fish was a Java edit and every card's id was assigned by a static counter
 * at construction time - so ids shifted between runs and could not be relied on. The data
 * now lives in the CSV and carries its own stable id per species.
 *
 * Columns are located by NAME from the header row, not by position, so extra columns can be
 * added to the CSV without touching this class. Required: id, name, speed, size, danger.
 * Optional: image_url, description, attribution_text - all blank in the seed for now, filled
 * in later by the image pipeline and the lore pass.
 *
 * Anything malformed fails loudly at load rather than silently producing a short or wrong
 * deck: a missing file, a missing required column, a non-numeric stat, a blank name or a
 * duplicate id all throw.
 *
 * Stat ceilings are held by Sailfish (Speed 100), Whale Shark (Size 100) and Blue-Ringed
 * Octopus (Danger 100); these were flagged inline in the old hand-written list.
 */
public class DeckFactory
{
    private static final String SEED_RESOURCE = "/fish_seed.csv";

    private static final String COL_ID          = "id";
    private static final String COL_NAME        = "name";
    private static final String COL_SPEED       = "speed";
    private static final String COL_SIZE        = "size";
    private static final String COL_DANGER      = "danger";
    private static final String COL_IMAGE       = "image_url";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_ATTRIBUTION = "attribution_text";

    /** Parsed once; every returnDeck() hands back a fresh shuffled copy of it. */
    private static List<Card> template = null;

    public static ArrayList<Card> returnDeck()
    {
        ArrayList<Card> deck = new ArrayList<>(loadTemplate());
        Collections.shuffle(deck);
        return deck;
    }

    private static synchronized List<Card> loadTemplate()
    {
        if (template == null) {
            template = parseSeed();
        }
        return template;
    }

    // ------------------------------------------------------------------ parsing

    private static List<Card> parseSeed()
    {
        List<Card> cards = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        try (InputStream in = DeckFactory.class.getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                    "Deck seed data not found on the classpath at " + SEED_RESOURCE
                    + ". It should live in src/main/resources.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException(SEED_RESOURCE + " is empty - expected a header row.");
            }
            Map<String, Integer> columns = readHeader(headerLine);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                List<String> fields = splitCsvLine(line);
                Card card = buildCard(fields, columns, lineNumber);

                if (!seenIds.add(card.getId())) {
                    throw new IllegalStateException(
                        SEED_RESOURCE + " line " + lineNumber + ": duplicate id " + card.getId()
                        + " - ids must be unique per species.");
                }
                cards.add(card);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + SEED_RESOURCE, e);
        }

        if (cards.isEmpty()) {
            throw new IllegalStateException(SEED_RESOURCE + " contained no card rows.");
        }
        return cards;
    }

    private static Map<String, Integer> readHeader(String headerLine)
    {
        // A UTF-8 BOM would otherwise become part of the first column's name.
        if (!headerLine.isEmpty() && headerLine.charAt(0) == 0xFEFF) {
            headerLine = headerLine.substring(1);
        }

        Map<String, Integer> columns = new HashMap<>();
        List<String> names = splitCsvLine(headerLine);
        for (int i = 0; i < names.size(); i++) {
            columns.put(names.get(i).trim().toLowerCase(), i);
        }

        for (String required : new String[] { COL_ID, COL_NAME, COL_SPEED, COL_SIZE, COL_DANGER }) {
            if (!columns.containsKey(required)) {
                throw new IllegalStateException(
                    SEED_RESOURCE + " is missing the required column '" + required
                    + "'. Found: " + names + ".");
            }
        }
        return columns;
    }

    private static Card buildCard(List<String> fields, Map<String, Integer> columns, int lineNumber)
    {
        String name = required(fields, columns, COL_NAME, lineNumber);
        if (name.isEmpty()) {
            throw new IllegalStateException(SEED_RESOURCE + " line " + lineNumber + ": name is blank.");
        }

        int id     = number(required(fields, columns, COL_ID, lineNumber),     COL_ID,     name, lineNumber);
        int speed  = number(required(fields, columns, COL_SPEED, lineNumber),  COL_SPEED,  name, lineNumber);
        int size   = number(required(fields, columns, COL_SIZE, lineNumber),   COL_SIZE,   name, lineNumber);
        int danger = number(required(fields, columns, COL_DANGER, lineNumber), COL_DANGER, name, lineNumber);

        return new Card(id, name, speed, size, danger,
                        optional(fields, columns, COL_IMAGE),
                        optional(fields, columns, COL_DESCRIPTION),
                        optional(fields, columns, COL_ATTRIBUTION));
    }

    private static String required(List<String> fields, Map<String, Integer> columns,
                                   String column, int lineNumber)
    {
        int index = columns.get(column);
        if (index >= fields.size()) {
            throw new IllegalStateException(
                SEED_RESOURCE + " line " + lineNumber + ": missing value for column '" + column
                + "' (row has " + fields.size() + " fields).");
        }
        return fields.get(index).trim();
    }

    /** Absent or blank optional columns become "" - never null, so callers need no guard. */
    private static String optional(List<String> fields, Map<String, Integer> columns, String column)
    {
        Integer index = columns.get(column);
        if (index == null || index >= fields.size()) return "";
        return fields.get(index).trim();
    }

    private static int number(String raw, String column, String name, int lineNumber)
    {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                SEED_RESOURCE + " line " + lineNumber + " (" + name + "): column '" + column
                + "' is not a whole number: '" + raw + "'.", e);
        }
    }

    /**
     * Split one CSV line. Handles double-quoted fields containing commas and doubled ("")
     * quotes - the lore descriptions that land in this file later will contain commas.
     */
    private static List<String> splitCsvLine(String line)
    {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;                 // consume the escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
