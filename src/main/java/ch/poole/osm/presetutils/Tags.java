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
    public static final Set<String> OBJECT_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("advertising", "aerialway", "aeroway", "amenity",
            "area:highway", "attraction", "barrier", "boundary", "building", "building:part", "cemetry", "club", "craft", "disc_golf", "departures_board",
            "emergency", "entrance", "ford", "geological", "golf", "harbour", "healthcare", "highway", "historic", "landcover", "landuse", "leisure", "indoor",
            "man_made", "military", "mountain_pass", "natural", "office", "pipeline", "piste:type", "place", "playground", "police", "power",
            "public_transport", "railway", "roof:edge", "roof:ridge", "seamark:type", "shop", "telecom", "tourism", "traffic_calming", "traffic_sign",
            "traffic_sign:backward", "traffic_sign:forward", "type", "waterway",
            /* the following are those top level keys that make sense in combination with lifecycle keys */
            "abandoned:aerialway", "abandoned:aeroway", "abandoned:amenity", "abandoned:attraction", "abandoned:building", "abandoned:cemetry",
            "abandoned:harbour", "abandoned:healthcare", "abandoned:highway", "abandoned:leisure", "abandoned:man_made", "abandoned:military",
            "abandoned:pipeline", "abandoned:place", "abandoned:playground", "abandoned:police", "abandoned:power", "abandoned:public_transport",
            "abandoned:railway", "abandoned:seamark:type", "abandoned:shop", "abandoned:telecom", "abandoned:tourism", "construction:aerialway",
            "construction:aeroway", "construction:amenity", "construction:attraction", "construction:building", "construction:cemetry", "construction:harbour",
            "construction:healthcare", "construction:highway", "construction:leisure", "construction:man_made", "construction:military",
            "construction:pipeline", "construction:place", "construction:playground", "construction:police", "construction:power",
            "construction:public_transport", "construction:railway", "construction:seamark:type", "construction:shop", "construction:telecom",
            "construction:tourism", "disused:aerialway", "disused:aeroway", "disused:amenity", "disused:attraction", "disused:building", "disused:cemetry",
            "disused:harbour", "disused:healthcare", "disused:highway", "disused:leisure", "disused:man_made", "disused:military", "disused:pipeline",
            "disused:place", "disused:playground", "disused:police", "disused:power", "disused:public_transport", "disused:railway", "disused:seamark:type",
            "disused:shop", "disused:telecom", "disused:tourism", "planned:aerialway", "planned:aeroway", "planned:amenity", "planned:attraction",
            "planned:building", "planned:cemetry", "planned:harbour", "planned:healthcare", "planned:highway", "planned:leisure", "planned:man_made",
            "planned:military", "planned:pipeline", "planned:place", "planned:playground", "planned:police", "planned:power", "planned:public_transport",
            "planned:railway", "planned:seamark:type", "planned:shop", "planned:telecom", "planned:tourism", "proposed:aerialway", "proposed:aeroway",
            "proposed:amenity", "proposed:attraction", "proposed:building", "proposed:cemetry", "proposed:harbour", "proposed:healthcare", "proposed:highway",
            "proposed:leisure", "proposed:man_made", "proposed:military", "proposed:pipeline", "proposed:place", "proposed:playground", "proposed:police",
            "proposed:power", "proposed:public_transport", "proposed:railway", "proposed:seamark:type", "proposed:shop", "proposed:telecom",
            "proposed:tourism")));

    /**
     * Tags that are exceptions
     */
    public static final Map<String, String> OBJECT_TAGS = new HashMap<>();
    static {
        OBJECT_TAGS.put("cycleway", "asl");
        OBJECT_TAGS.put("junction", "yes");
    }

    /**
     * Special keys that are used to indicate a special state
     */
    public static final Set<String> LIFECYCLE_KEYS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("abandoned", "construction", "disused", "planned", "proposed")));

    /**
     * Mapping of some 2nd level keys that are not the value of the top level key
     */
    public static final Map<String, String> SECOND_LEVEL_KEYS = new HashMap<>();
    static {
        SECOND_LEVEL_KEYS.put("vending_machine", "vending");
        SECOND_LEVEL_KEYS.put("stadium", "sport");
        SECOND_LEVEL_KEYS.put("pitch", "sport");
        SECOND_LEVEL_KEYS.put("track", "sport");
        SECOND_LEVEL_KEYS.put("sports_centre", "sport");
        SECOND_LEVEL_KEYS.put("ice_rink", "sport");
        SECOND_LEVEL_KEYS.put("raceway", "sport");
        SECOND_LEVEL_KEYS.put("restaurant", "cuisine");
        SECOND_LEVEL_KEYS.put("fast_food", "cuisine");
        SECOND_LEVEL_KEYS.put("artwork", "artwork_type");
        SECOND_LEVEL_KEYS.put("shelter", "shelter_type");
        SECOND_LEVEL_KEYS.put("castle", "castle_type");
        SECOND_LEVEL_KEYS.put("fence", "fence_type");
        SECOND_LEVEL_KEYS.put("tower", "tower:type");
        SECOND_LEVEL_KEYS.put("mast", "tower:type");
        SECOND_LEVEL_KEYS.put("storage_tank", "content");
        SECOND_LEVEL_KEYS.put("garden", "garden:type");
        SECOND_LEVEL_KEYS.put("townhall", "townhall:type");
        SECOND_LEVEL_KEYS.put("crane", "crane:type");
        SECOND_LEVEL_KEYS.put("aerodrome", "aerodrome:type");
    }

    public static final Map<String, String> KEYS_FOR_SPECIFIC_ELEMENT = new HashMap<>();
    static {
        KEYS_FOR_SPECIFIC_ELEMENT.put("type", "relations");
    }

    /**
     * Attributes that are never 2nd level keys
     */
    public static final Set<String> NOT_SECOND_LEVEL_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("phone", "health", "census", "postal_code", "maxspeed", "designated", "heritage", "incline", "network", "level",
                    "motorcycle", "bicycle", "snowmobile", "organic", "organic_only", "fireplace", "boat", "bar", "compressed_air", "swimming_pool", "taxi",
                    "atm", "telephone", "waste_basket", "drinking_water", "sanitary_dump_station", "water_point", "biergarten", "bench", "give_way", "access",
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
        NOT_SECOND_LEVEL_KEYS_2.add("public_transport=shelter", new String[] { "shelter", "shelter_type" });
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
                new String[] { "cycleway=lane", "cycleway=track", "cycleway=opposite", "cycleway=opposite_lane", "cycleway=shared_lane", "cycleway=asl" });
        NOT_SECOND_LEVEL_KEYS_2.add("amenity=police", "police");
        NOT_SECOND_LEVEL_KEYS_2.add("highway=track", "sport");
        NOT_SECOND_LEVEL_KEYS_2.add("area:highway=track", "sport");
    }

    public static final Set<String> NOT_OBJECT_KEY_VALUES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("yes", "no", "only", "true", "false", "1", "-1", "null")));
}
