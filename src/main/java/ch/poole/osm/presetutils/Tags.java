package ch.poole.osm.presetutils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Tags {
    /**
     * An set of tags considered 'important'. These are typically tags that define real-world objects and not properties
     * of such.
     */
    public static final Set<String> OBJECT_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("aerialway", "aeroway", "amenity", "barrier",
            "boundary", "building", "craft", "emergency", "ford", "geological", "highway", "historic", "landuse", "leisure", "man_made", "military", "natural",
            "office", "place", "power", "public_transport", "railway", "shop", "tourism", "waterway", "type", "entrance", "pipeline", "healthcare",
            "playground", "attraction", "traffic_sign", "traffic_sign:forward", "traffic_sign:backward", "golf", "indoor", "cemetry", "building:part",
            "landcover", "advertising", "traffic_calming", "club", "cemetery", "police")));

    /**
     * Special keys that are used to indicate a special mostly temporary state
     */
    public static final Set<String> TEMP_KEYS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("abandoned", "construction", "disused", "planned", "proposed")));

    /**
     * Mapping of some 2nd level keys that are not the value of the top level key
     */
    public static final Map<String, String> SECOND_LEVEL_KEYS = new HashMap<>();
    static {
        SECOND_LEVEL_KEYS.put("vending_machine", "vending");
        SECOND_LEVEL_KEYS.put("stadium", "sport");
        SECOND_LEVEL_KEYS.put("pitch", "sport");
        SECOND_LEVEL_KEYS.put("sports_centre", "sport");
        SECOND_LEVEL_KEYS.put("restaurant", "cuisine");
        SECOND_LEVEL_KEYS.put("artwork", "artwork_type");
        SECOND_LEVEL_KEYS.put("shelter", "shelter_type");
        SECOND_LEVEL_KEYS.put("castle", "castle_type");
        SECOND_LEVEL_KEYS.put("fence", "fence_type");
        // SECOND_LEVEL_KEYS.put("tower", "tower:type");
        // SECOND_LEVEL_KEYS.put("mast", "tower:type");
    }

    public static final Map<String, String> KEYS_FOR_SPECIFIC_ELEMENT = new HashMap<>();
    static {
        KEYS_FOR_SPECIFIC_ELEMENT.put("type", "relations");
    }

    /**
     * Attributes that are never 2nd level keys
     */
    public static final Set<String> NOT_SECOND_LEVEL_KEYS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("phone", "health", "census", "postal_code", "maxspeed", "designated", "heritage", "incline", "network",
                    "level", "motorcycle", "bicycle", "snowmobile", "organic", "fireplace", "boat", "bar", "compressed_air", "swimming_pool", "taxi", "atm",
                    "telephone", "waste_basket", "drinking_water", "sanitary_dump_station", "water_point", "biergarten", "bench", "give_way", "access",
                    "noexit", "outdoor_seating", "goods", "second_hand", "atv", "tobacco", "household", "ski", "ice_cream", "vacant", "car", "fishing",
                    "toilet", "shelter", "handrail", "monorail", "unisex", "private", "exit", "video", "window", "laundry", "table", "steps", "fixme")));

    /**
     * 2nd level keys/tags that are not used with certain tags
     */
    public static final MultiHashMap<String, String> NOT_SECOND_LEVEL_KEYS_2 = new MultiHashMap<>();
    static {
        NOT_SECOND_LEVEL_KEYS_2.add("amenity=water", "water");
        NOT_SECOND_LEVEL_KEYS_2.add("amenity=restaurant", "restaurant");
        NOT_SECOND_LEVEL_KEYS_2.add("building=service", "service");
        NOT_SECOND_LEVEL_KEYS_2.add("entrance=service", "service");
        NOT_SECOND_LEVEL_KEYS_2.add("aeroway=aerodrome", "aerodrome");
        NOT_SECOND_LEVEL_KEYS_2.add("building:part=room", "room");
        NOT_SECOND_LEVEL_KEYS_2.add("railway=crossing", "crossing");
        NOT_SECOND_LEVEL_KEYS_2.add("railway=site", "site");
        NOT_SECOND_LEVEL_KEYS_2.add("railway=switch", "switch");
        NOT_SECOND_LEVEL_KEYS_2.add("indoor=wall", "wall");
        NOT_SECOND_LEVEL_KEYS_2.add("aeroway=marking", new String[] { "marking=zebra", "marking=unmarked", "marking=sides", "marking=sport" });
        NOT_SECOND_LEVEL_KEYS_2.add("public_transport=pole", "pole");
        NOT_SECOND_LEVEL_KEYS_2.add("public_transport=shelter", "shelter");
        NOT_SECOND_LEVEL_KEYS_2.add("traffic_sign=maxweight", "maxweight");
        NOT_SECOND_LEVEL_KEYS_2.add("traffic_sign=maxspeed:advisory", "maxspeed:advisory");
        NOT_SECOND_LEVEL_KEYS_2.add("highway=service",
                new String[] { "service=repair", "service=access", "service=yard", "service=crossover", "service=slipway", "service=commuter", "service=fuel",
                        "service=spur", "service=resource extraction", "service=irrigation", "service=siding", "service=water_power", "service=tyres",
                        "service=agricultural", "service=long_distance", "service=parts", "service=regional", "service=dealer" });
        NOT_SECOND_LEVEL_KEYS_2.add("attraction=animal", "animal=shelter");
        NOT_SECOND_LEVEL_KEYS_2.add("historic=shelter", "shelter");
        NOT_SECOND_LEVEL_KEYS_2.add("building=residential", "residential");
        NOT_SECOND_LEVEL_KEYS_2.add("highway=residential", "residential");
        NOT_SECOND_LEVEL_KEYS_2.add("highway=cycleway",
                new String[] { "cycleway=lane", "cycleway=track", "cycleway=opposite", "cycleway=opposite_lane", "cycleway=shared_lane" });
        NOT_SECOND_LEVEL_KEYS_2.add("amenity=police", "police");
    }

    public static final Set<String> NOT_OBJECT_KEY_VALUES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("yes", "no", "only", "true", "false", "1", "-1", "null")));
}
