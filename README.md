# Preset utils

A couple of utils for processing JOSM format presets for OSM editors and related stuff.

The repo further contains all Vespucci preset xlmns files, a copy of the JOSM one (JOSM currently doesn't version the file), and a number of xslt scripts to convert preset files. In particular src/main/resources/toJOSM.xslt will convert a preset file using the extensions supported by Vespucci to a JOSM compatible one (naturally the functionality will be loss too).

To run any of the commands below get the [fat jar from the releases](https://github.com/simonpoole/preset-utils/releases/tag/0.38.0) in this repository and then run

    java -cp preset-utils-all-0.38.0.jar ch.poole.osm.presetutils.......
  
for example

    java -cp preset-utils-all-0.38.0.jar ch.poole.osm.presetutils.Preset2Pot -h

## Preset2Pot

Generates a [gettext](https://www.gnu.org/software/gettext/) format translation template file from the preset file.

### Usage

    -i,--input <arg>    input preset file, default: standard in
    -o,--output <arg>   output .pot file, default: standard out

## Preset2Html

Generate a simple HTML page displaying the contents of the preset file.

Replaces 

__${ICONPATH}__ with _icons/png/_

__${ICONTYPE}__ with _.png_

with other words it assumes that the icons are available in PNG format in _icons/png_.

Further it expects _preset.css_ in the directory _website_

### Usage

    -i,--input <arg>      input preset file, default: standard in
    -j,--josm <arg>       download link JOSM format, default: none
    -o,--output <arg>     output .html file, default: standard out
    -v,--vespucci <arg>   download link vespucci format, default: none
    
## Synonyms

Retrieve synonym values from the iD editor repository and output a json object containing them.

### Usage
      
     -u,--url <arg>        base url, required
     -l,--lang <arg>       language code to retrieve the synonyms for, default: en
     -o,--output <arg>     output json file, default: standard out
     -x,--exclude <args>   skip any entries that contain the terms (for example "military")
     -r,--remove           remove empty output files
     
## ID2JOSM

Generate a JOSM preset from the iD preset configuration, retrieving some values from taginfo similar to what iD does.

Note: 
- this does not include any icons from the preset (but it does include references) and while it is mainly useful for statistical and comparison purposes, the output is fully functional.
- if querying taginfo is not turned off, a fair bit of debugging output is written and the querying is rate limited (aka slow).

Currently ignored iD preset features are "matchScore", "countryCodes", "replacement", "removeTags", "icon", "imageURL" and "terms" (the Synonym tool can be used to turn the term field in to something useful).

### Usage

     -o,--output <arg>     output xml file, default: standard out
     -c,--chunk            if set iD field definitions will be converted to JOSM preset "chunks", if not set the fields
                           are expanded inline
     -n,--notaginfo        don't query taginfo for values and keys
     -j,--josmonly         don't use Vespucci extensions to the JOSM preset format
     -f,--fieldsurl        url to file containing field definitions, default those in the id-tagging-schema repository
     -p,--preseturl        url to file containing preset definitions, default those in the id-tagging-schema repository
     -t,--translationurl   url to file containing translations, default those in the id-tagging-schema repository
     
## PresetStats

Generate a list of tag combinations (first and 2nd level) from the presets and generate some stats on number of keys and values.

### Usage

    -i,--input <arg>      input preset file, default: standard in
    -o,--output <arg>     output file, default: standard out
    -t,--taginfo          query taginfo for tag counts, default: off
    -d,--ignoredeprecated ignore deprecated items, default: false
    
## MergePresetStats

Combine a taglist generated by PresetStats for multiple presets

### Usage

    -i,--input <args>     input preset stats files (at least one)
    -o,--output <arg>     output file, default: standard out

## TagsFromTaginfo

Generate a list of tag combinations (first and 2nd level) in the format generated by PresetStats from taginfo

### Usage

    -o,--output <arg>     output file, default: standard out
    -m,--minimum <arg>    minimum occurrence count, subtags have to reach at least 1/5 of this default: 500
    -n,--nosubtags        don't query and output subtags
    
## ComparePresets

Compare two tag lists in the format generated by PresetStats

### Usage

    -i,--input <args>     input preset stats file
    -r,--reference <arg>  reference preset stats file
    -d,--depth <arg>      number of hierarchy steps to consider, 0 = all and is the default
    
## CheckPreset

Do some consistency checks on a preset, this should be used additionally to validation against the xlmns specification. 

### Usage

    -i,--input <args>     input preset file

Issues will be logged and in case of an error a non-zero status is returned.